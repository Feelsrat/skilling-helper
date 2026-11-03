/*
 * SkillingHelper.exe: the program GameNative starts for the "Skilling Helper" custom game.
 *
 * GameNative only picks a game's program by itself when the game folder holds exactly one .exe, and it has no way
 * to be given launch arguments from outside. This launcher is that one .exe. Everything else sits next to it:
 *
 *   jre\               a Windows Java 21
 *   client.jar         the RuneLite dev client with the plugins built in
 *   version.txt        which release client.jar is
 *   home\.runelite\    RuneLite's settings and the saved Jagex login
 *
 * (The game has to carry its own files: GameNative isn't allowed to read files other apps saved on the phone.)
 *
 * On each start it checks the newest release's android-version.txt and, if it differs from version.txt, downloads
 * android-client.jar over client.jar. Any failure there just starts the client.jar it already has. Then it runs
 * jre\bin\javaw.exe -jar client.jar with RuneLite's settings kept in home\.
 *
 * Java's output (RuneLite's log) goes to Download/SkillingHelper-log.txt on the phone when GameNative may write
 * there, otherwise launcher.log next to this .exe.
 *
 * Built without the C runtime (clang + lld-link, see build.sh), so it is a few KB and needs no MinGW install.
 */

typedef unsigned short WCHAR;
typedef int BOOL;
typedef unsigned long DWORD;
typedef void *HANDLE;
typedef long HRESULT;

typedef struct {
	DWORD cb;
	WCHAR *lpReserved, *lpDesktop, *lpTitle;
	DWORD dwX, dwY, dwXSize, dwYSize, dwXCountChars, dwYCountChars, dwFillAttribute, dwFlags;
	unsigned short wShowWindow, cbReserved2;
	unsigned char *lpReserved2;
	HANDLE hStdInput, hStdOutput, hStdError;
} STARTUPINFOW;

typedef struct {
	HANDLE hProcess, hThread;
	DWORD dwProcessId, dwThreadId;
} PROCESS_INFORMATION;

__declspec(dllimport) DWORD __stdcall GetModuleFileNameW(HANDLE, WCHAR *, DWORD);
__declspec(dllimport) BOOL __stdcall CreateProcessW(const WCHAR *, WCHAR *, void *, void *, BOOL, DWORD, void *,
	const WCHAR *, STARTUPINFOW *, PROCESS_INFORMATION *);
__declspec(dllimport) DWORD __stdcall WaitForSingleObject(HANDLE, DWORD);
__declspec(dllimport) BOOL __stdcall GetExitCodeProcess(HANDLE, DWORD *);
__declspec(dllimport) DWORD __stdcall GetFileAttributesW(const WCHAR *);
__declspec(dllimport) HANDLE __stdcall CreateFileW(const WCHAR *, DWORD, DWORD, void *, DWORD, DWORD, HANDLE);
__declspec(dllimport) BOOL __stdcall ReadFile(HANDLE, void *, DWORD, DWORD *, void *);
__declspec(dllimport) BOOL __stdcall WriteFile(HANDLE, const void *, DWORD, DWORD *, void *);
__declspec(dllimport) BOOL __stdcall CloseHandle(HANDLE);
__declspec(dllimport) DWORD __stdcall GetFileSize(HANDLE, DWORD *);
__declspec(dllimport) BOOL __stdcall MoveFileExW(const WCHAR *, const WCHAR *, DWORD);
__declspec(dllimport) BOOL __stdcall DeleteFileW(const WCHAR *);
__declspec(dllimport) BOOL __stdcall CreateDirectoryW(const WCHAR *, void *);
__declspec(dllimport) DWORD __stdcall GetTickCount(void);
__declspec(dllimport) void __stdcall ExitProcess(unsigned int);
__declspec(dllimport) int __stdcall MessageBoxW(HANDLE, const WCHAR *, const WCHAR *, unsigned int);
__declspec(dllimport) HRESULT __stdcall URLDownloadToFileW(void *, const WCHAR *, const WCHAR *, DWORD, void *);

#define INFINITE 0xFFFFFFFFu
#define INVALID_FILE_ATTRIBUTES 0xFFFFFFFFu
#define INVALID_HANDLE_VALUE ((HANDLE)(long long)-1)
#define GENERIC_READ 0x80000000u
#define GENERIC_WRITE 0x40000000u
#define CREATE_ALWAYS 2u
#define STARTF_USESTDHANDLES 0x100u
#define FILE_SHARE_READ 1u
#define OPEN_EXISTING 3u
#define MOVEFILE_REPLACE_EXISTING 1u
#define MB_ICONERROR 0x10u

#define RELEASES L"https://github.com/Feelsrat/skilling-helper/releases/latest/download/"
/* The client is ~34 MB; anything much smaller is an error page, not a jar */
#define MIN_JAR_SIZE 1000000u

static const WCHAR TITLE[] = L"Skilling Helper";
static WCHAR dir[1024]; /* folder this .exe is in, with a trailing backslash */
static HANDLE log_file = INVALID_HANDLE_VALUE;
static WCHAR *path(WCHAR *out, const WCHAR *name);

typedef struct {
	DWORD nLength;
	void *lpSecurityDescriptor;
	BOOL bInheritHandle;
} SECURITY_ATTRIBUTES;

static void log_text(const char *s)
{
	if (log_file == INVALID_HANDLE_VALUE)
	{
		return;
	}
	DWORD n = 0, len = 0;
	while (s[len])
	{
		len++;
	}
	WriteFile(log_file, s, len, &n, 0);
}

/* Java's output goes to a log you can open on the phone: the Download folder (drive D:) when GameNative may
   write there, otherwise launcher.log next to this .exe. The handle is inheritable so Java writes into it. */
static void open_log(void)
{
	static WCHAR local[1100];
	SECURITY_ATTRIBUTES sa = {sizeof(SECURITY_ATTRIBUTES), 0, 1};
	log_file = CreateFileW(L"D:\\SkillingHelper-log.txt", GENERIC_WRITE, FILE_SHARE_READ, &sa, CREATE_ALWAYS, 0, 0);
	if (log_file == INVALID_HANDLE_VALUE)
	{
		log_file = CreateFileW(path(local, L"launcher.log"), GENERIC_WRITE, FILE_SHARE_READ, &sa, CREATE_ALWAYS, 0, 0);
	}
	log_text("SkillingHelper launcher started\r\n");
}

static WCHAR *append(WCHAR *p, const WCHAR *s)
{
	while (*s)
	{
		*p++ = *s++;
	}
	*p = 0;
	return p;
}

/* out = dir + name */
static WCHAR *path(WCHAR *out, const WCHAR *name)
{
	append(append(out, dir), name);
	return out;
}

static void fail(const WCHAR *msg)
{
	MessageBoxW(0, msg, TITLE, MB_ICONERROR);
	ExitProcess(1);
}

/* Reads up to cap-1 bytes of a small file; returns the length, or -1 if it can't be read */
static int read_small(const WCHAR *file, char *buf, int cap)
{
	HANDLE h = CreateFileW(file, GENERIC_READ, FILE_SHARE_READ, 0, OPEN_EXISTING, 0, 0);
	if (h == INVALID_HANDLE_VALUE)
	{
		return -1;
	}
	DWORD n = 0;
	BOOL ok = ReadFile(h, buf, (DWORD)(cap - 1), &n, 0);
	CloseHandle(h);
	if (!ok)
	{
		return -1;
	}
	/* Trim trailing whitespace/newlines */
	while (n > 0 && (buf[n - 1] == '\n' || buf[n - 1] == '\r' || buf[n - 1] == ' '))
	{
		n--;
	}
	buf[n] = 0;
	return (int)n;
}

static DWORD file_size(const WCHAR *file)
{
	HANDLE h = CreateFileW(file, GENERIC_READ, FILE_SHARE_READ, 0, OPEN_EXISTING, 0, 0);
	if (h == INVALID_HANDLE_VALUE)
	{
		return 0;
	}
	DWORD size = GetFileSize(h, 0);
	CloseHandle(h);
	return size == 0xFFFFFFFFu ? 0 : size;
}

static void write_hex(WCHAR *p, DWORD v)
{
	static const WCHAR hex[] = L"0123456789abcdef";
	for (int i = 7; i >= 0; i--)
	{
		*p++ = hex[(v >> (i * 4)) & 15];
	}
	*p = 0;
}

static int same(const char *a, const char *b)
{
	while (*a && *a == *b)
	{
		a++;
		b++;
	}
	return *a == *b;
}

/* Download RELEASES + asset to dest; a changing query string keeps Wine's download cache from answering */
static int download(const WCHAR *asset, const WCHAR *dest)
{
	WCHAR url[512];
	WCHAR *p = append(append(append(url, RELEASES), asset), L"?t=");
	write_hex(p, GetTickCount());
	DeleteFileW(dest);
	return URLDownloadToFileW(0, url, dest, 0, 0) == 0;
}

/* Swap in the newest SkillingHelper.exe for the next start. A running .exe can be renamed but not overwritten. */
static void update_self(void)
{
	static WCHAR self[1100], new_self[1100], old_self[1100];
	char head[4];
	path(self, L"SkillingHelper.exe");
	path(new_self, L"SkillingHelper.exe.new");
	path(old_self, L"SkillingHelper.exe.old");
	DeleteFileW(old_self);
	if (!download(L"SkillingHelper.exe", new_self) || file_size(new_self) < 1024
		|| read_small(new_self, head, sizeof head) < 2 || head[0] != 'M' || head[1] != 'Z')
	{
		DeleteFileW(new_self);
		return;
	}
	if (MoveFileExW(self, old_self, MOVEFILE_REPLACE_EXISTING))
	{
		if (MoveFileExW(new_self, self, MOVEFILE_REPLACE_EXISTING))
		{
			log_text("Launcher updated for the next start\r\n");
			return;
		}
		MoveFileExW(old_self, self, MOVEFILE_REPLACE_EXISTING);
	}
	DeleteFileW(new_self);
}

static void update(void)
{
	static WCHAR local_ver[1100], new_ver[1100], jar[1100], new_jar[1100];
	char have[128], latest[128];
	path(local_ver, L"version.txt");
	path(new_ver, L"version.txt.new");
	path(jar, L"client.jar");
	path(new_jar, L"client.jar.new");

	if (!download(L"android-version.txt", new_ver) || read_small(new_ver, latest, sizeof latest) <= 0)
	{
		DeleteFileW(new_ver);
		log_text("Update check failed (offline or GitHub unreachable); starting the client already here\r\n");
		return; /* offline or GitHub unreachable: play what we have */
	}
	if (read_small(local_ver, have, sizeof have) > 0 && same(have, latest)
		&& GetFileAttributesW(jar) != INVALID_FILE_ATTRIBUTES)
	{
		DeleteFileW(new_ver);
		log_text("Client is up to date: ");
		log_text(have);
		log_text("\r\n");
		return;
	}
	log_text("Downloading client ");
	log_text(latest);
	log_text("\r\n");
	if (download(L"android-client.jar", new_jar) && file_size(new_jar) >= MIN_JAR_SIZE
		&& MoveFileExW(new_jar, jar, MOVEFILE_REPLACE_EXISTING))
	{
		MoveFileExW(new_ver, local_ver, MOVEFILE_REPLACE_EXISTING);
		log_text("Client updated\r\n");
		update_self();
	}
	else
	{
		log_text("Client download failed; starting the client already here\r\n");
		DeleteFileW(new_jar);
		DeleteFileW(new_ver);
	}
}

void start(void)
{
	static WCHAR java[1100], jar[1100], home[1100], cmd[4096];

	DWORD n = GetModuleFileNameW(0, dir, 900);
	if (n == 0 || n >= 900)
	{
		fail(L"Couldn't find where SkillingHelper.exe is.");
	}
	/* Cut off the file name, keep the folder and its backslash */
	WCHAR *slash = 0;
	for (WCHAR *p = dir; *p; p++)
	{
		if (*p == L'\\' || *p == L'/')
		{
			slash = p;
		}
	}
	if (!slash)
	{
		fail(L"Couldn't find where SkillingHelper.exe is.");
	}
	slash[1] = 0;

	path(java, L"jre\\bin\\javaw.exe");
	path(jar, L"client.jar");
	path(home, L"home");
	CreateDirectoryW(home, 0);
	open_log();

	if (GetFileAttributesW(java) == INVALID_FILE_ATTRIBUTES)
	{
		fail(L"Java (jre\\bin\\javaw.exe) is missing next to SkillingHelper.exe. "
			L"Run skilling-helper-android in Termux and import the SkillingHelper folder into GameNative again.");
	}

	update();

	if (GetFileAttributesW(jar) == INVALID_FILE_ATTRIBUTES)
	{
		fail(L"client.jar is missing and couldn't be downloaded. Check the internet connection and start again.");
	}

	WCHAR *p = append(cmd, L"\"");
	p = append(p, java);
	/* Java's compiled code crashed under GameNative's x86 emulation (EXCEPTION_ACCESS_VIOLATION in C1 code).
	   HotSpot normally turns null checks into deliberate memory faults, which an emulator can mishandle, so use
	   explicit checks, and the simpler C1 compiler only. Crash reports go next to the log in Download.
	   The next crash was in a vtable stub decoding a compressed class pointer, so use full 64-bit pointers.
	   Serial GC keeps Java to one GC thread (less emulation work), and Java2D's Direct3D path is off.
	   Crashes kept moving around, so also avoid AVX/BMI/FMA, which the emulator advertises but may mis-emulate. */
	p = append(p, L"\" -ea -Xmx1g -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions"
		L" -XX:-ImplicitNullChecks -XX:TieredStopAtLevel=1 -XX:-UseCompressedOops -XX:-UseCompressedClassPointers"
		L" -XX:+UseSerialGC -XX:UseAVX=0 -XX:-UseBMI1Instructions -XX:-UseBMI2Instructions -XX:-UseFMA"
		L" -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true"
		L" -XX:ErrorFile=D:\\SkillingHelper-crash-%p.log"
		L" \"-Duser.home=");
	p = append(p, home);
	p = append(p, L"\" -jar \"");
	p = append(p, jar);
	append(p, L"\" --developer-mode --debug");
	log_text("Starting Java\r\n");

	STARTUPINFOW si = {0};
	PROCESS_INFORMATION pi = {0};
	si.cb = sizeof(si);
	BOOL inherit = 0;
	if (log_file != INVALID_HANDLE_VALUE)
	{
		si.dwFlags = STARTF_USESTDHANDLES;
		si.hStdOutput = log_file;
		si.hStdError = log_file;
		inherit = 1;
	}
	if (!CreateProcessW(java, cmd, 0, 0, inherit, 0, 0, dir, &si, &pi))
	{
		fail(L"Couldn't start Java.");
	}
	WaitForSingleObject(pi.hProcess, INFINITE);
	DWORD code = 0;
	GetExitCodeProcess(pi.hProcess, &code);
	ExitProcess(code);
}
