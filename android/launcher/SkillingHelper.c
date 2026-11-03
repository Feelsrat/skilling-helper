/*
 * SkillingHelper.exe: the program GameNative starts for the "Skilling Helper" custom game.
 *
 * GameNative only picks a game's program by itself when the game folder holds exactly one .exe, and it has no way
 * to be given launch arguments from outside. This launcher is that one .exe: it starts the Windows Java that sits
 * next to it (jre\bin\javaw.exe) with the arguments the RuneLite dev client needs, so nothing has to be typed into
 * GameNative's settings. The jar and RuneLite's settings live in the phone's Download folder, which GameNative
 * shows to Windows as drive D:.
 *
 * Built without the C runtime (clang + lld-link, see build.sh), so it is a few KB and needs no MinGW install.
 */

typedef unsigned short WCHAR;
typedef int BOOL;
typedef unsigned long DWORD;
typedef void *HANDLE;

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
__declspec(dllimport) void __stdcall ExitProcess(unsigned int);
__declspec(dllimport) int __stdcall MessageBoxW(HANDLE, const WCHAR *, const WCHAR *, unsigned int);

#define INFINITE 0xFFFFFFFFu
#define INVALID_FILE_ATTRIBUTES 0xFFFFFFFFu
#define MB_ICONERROR 0x10u

static const WCHAR JAR[] = L"D:\\skilling-helper\\skilling-helper-all.jar";
static const WCHAR ARGS[] = L" -ea -Xmx1g -Duser.home=D:\\skilling-helper\\home -jar "
	L"D:\\skilling-helper\\skilling-helper-all.jar --developer-mode";
static const WCHAR TITLE[] = L"Skilling Helper";

static WCHAR *append(WCHAR *p, const WCHAR *s)
{
	while (*s)
	{
		*p++ = *s++;
	}
	*p = 0;
	return p;
}

static void fail(const WCHAR *msg)
{
	MessageBoxW(0, msg, TITLE, MB_ICONERROR);
	ExitProcess(1);
}

void start(void)
{
	static WCHAR java[1024];
	static WCHAR cmd[2048];

	DWORD n = GetModuleFileNameW(0, java, 900);
	if (n == 0 || n >= 900)
	{
		fail(L"Couldn't find where SkillingHelper.exe is.");
	}
	/* Cut off the file name, keep the folder */
	WCHAR *slash = 0;
	for (WCHAR *p = java; *p; p++)
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
	append(slash + 1, L"jre\\bin\\javaw.exe");

	if (GetFileAttributesW(java) == INVALID_FILE_ATTRIBUTES)
	{
		fail(L"Java is missing next to SkillingHelper.exe. Run skilling-helper-android --reinstall-java in Termux.");
	}
	if (GetFileAttributesW(JAR) == INVALID_FILE_ATTRIBUTES)
	{
		fail(L"D:\\skilling-helper\\skilling-helper-all.jar is missing. Run skilling-helper-android in Termux, "
			L"and check the game's D: drive is the phone's Download folder.");
	}

	WCHAR *p = append(cmd, L"\"");
	p = append(p, java);
	p = append(p, L"\"");
	append(p, ARGS);

	STARTUPINFOW si = {0};
	PROCESS_INFORMATION pi = {0};
	si.cb = sizeof(si);
	if (!CreateProcessW(java, cmd, 0, 0, 0, 0, 0, 0, &si, &pi))
	{
		fail(L"Couldn't start Java.");
	}
	WaitForSingleObject(pi.hProcess, INFINITE);
	DWORD code = 0;
	GetExitCodeProcess(pi.hProcess, &code);
	ExitProcess(code);
}
