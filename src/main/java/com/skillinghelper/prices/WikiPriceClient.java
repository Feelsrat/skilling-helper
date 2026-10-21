package com.skillinghelper.prices;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Real-time Grand Exchange prices from the OSRS Wiki prices API (https://prices.runescape.wiki). Only used when the
 * player opts in, since it contacts a third-party server. Requests run on the OkHttp thread pool.
 */
@Slf4j
@Singleton
public class WikiPriceClient
{
	private static final String LATEST_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
	// The wiki asks every client to identify itself
	private static final String USER_AGENT = "skilling-helper RuneLite plugin (github.com/Feelsrat/skilling-helper)";

	@Value
	public static class Price
	{
		/**
		 * Latest instant-buy price, 0 if unknown.
		 */
		int high;
		/**
		 * Latest instant-sell price, 0 if unknown.
		 */
		int low;
	}

	private final OkHttpClient http;
	private final Gson gson;

	private volatile Map<Integer, Price> prices = Collections.emptyMap();
	private volatile Instant updated;
	private volatile Call inFlight;

	@Inject
	WikiPriceClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	/**
	 * Starts a refresh in the background unless one is already running.
	 */
	public void refresh()
	{
		if (inFlight != null)
		{
			return;
		}
		Request request = new Request.Builder()
			.url(LATEST_URL)
			.header("User-Agent", USER_AGENT)
			.build();
		Call call = http.newCall(request);
		inFlight = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				inFlight = null;
				log.debug("Unable to fetch wiki prices", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				inFlight = null;
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.debug("Wiki prices request failed: {}", response.code());
						return;
					}
					prices = parse(gson.fromJson(body.charStream(), JsonObject.class));
					updated = Instant.now();
				}
				catch (JsonParseException | IllegalStateException e)
				{
					log.debug("Unable to parse wiki prices", e);
				}
			}
		});
	}

	private static Map<Integer, Price> parse(JsonObject root)
	{
		JsonObject data = root == null ? null : root.getAsJsonObject("data");
		if (data == null)
		{
			return Collections.emptyMap();
		}
		Set<Map.Entry<String, JsonElement>> entries = data.entrySet();
		Map<Integer, Price> out = new HashMap<>(entries.size() * 2);
		for (Map.Entry<String, JsonElement> e : entries)
		{
			if (!e.getValue().isJsonObject())
			{
				continue;
			}
			JsonObject o = e.getValue().getAsJsonObject();
			try
			{
				out.put(Integer.parseInt(e.getKey()), new Price(intOrZero(o, "high"), intOrZero(o, "low")));
			}
			catch (NumberFormatException ignored)
			{
				// not an item id
			}
		}
		return out;
	}

	private static int intOrZero(JsonObject o, String key)
	{
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
	}

	public Price get(int itemId)
	{
		return prices.get(itemId);
	}

	public boolean hasPrices()
	{
		return !prices.isEmpty();
	}

	public Instant getUpdated()
	{
		return updated;
	}

	public void cancel()
	{
		Call call = inFlight;
		if (call != null)
		{
			call.cancel();
		}
		inFlight = null;
	}

	public void clear()
	{
		prices = Collections.emptyMap();
		updated = null;
	}
}
