package com.skillinghelper.money;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * How many of each item traded on the Grand Exchange in the last 24 hours, from the OSRS Wiki prices API. Only used
 * when the player opts in to live wiki prices, since it contacts a third-party server. Requests run on the OkHttp
 * thread pool.
 */
@Slf4j
@Singleton
class WikiVolumeClient
{
	private static final String URL = "https://prices.runescape.wiki/api/v1/osrs/volumes";
	// The wiki asks every client to identify itself
	private static final String USER_AGENT = "skilling-helper RuneLite plugin (github.com/Feelsrat/skilling-helper)";

	private final OkHttpClient http;
	private final Gson gson;
	private volatile Map<Integer, Long> volumes = Collections.emptyMap();
	private volatile Call inFlight;

	@Inject
	WikiVolumeClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	void refresh()
	{
		if (inFlight != null)
		{
			return;
		}
		Call call = http.newCall(new Request.Builder().url(URL).header("User-Agent", USER_AGENT).build());
		inFlight = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				inFlight = null;
				log.debug("Unable to fetch wiki trade volumes", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				inFlight = null;
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.debug("Wiki volumes request failed: {}", response.code());
						return;
					}
					volumes = parse(gson.fromJson(body.charStream(), JsonObject.class));
				}
				catch (JsonParseException | IllegalStateException | NumberFormatException e)
				{
					log.debug("Unable to parse wiki trade volumes", e);
				}
			}
		});
	}

	private static Map<Integer, Long> parse(JsonObject root)
	{
		JsonObject data = root == null ? null : root.getAsJsonObject("data");
		if (data == null)
		{
			return Collections.emptyMap();
		}
		Map<Integer, Long> out = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : data.entrySet())
		{
			if (e.getValue().isJsonPrimitive())
			{
				out.put(Integer.parseInt(e.getKey()), e.getValue().getAsLong());
			}
		}
		return out;
	}

	/**
	 * Daily volume by item id, empty until fetched.
	 */
	Map<Integer, Long> get()
	{
		return volumes;
	}

	void cancel()
	{
		Call call = inFlight;
		if (call != null)
		{
			call.cancel();
		}
		inFlight = null;
	}

	void clear()
	{
		volumes = Collections.emptyMap();
	}
}
