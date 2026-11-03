#!/usr/bin/env bash
# Builds SkillingHelper.exe (64-bit Windows, no C runtime) with clang and lld-link.
# Needs: clang, lld (lld-link), llvm (llvm-dlltool). Usage: build.sh OUTPUT.exe
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
out="$(realpath -m "${1:-SkillingHelper.exe}")"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
{ echo "LIBRARY kernel32.dll"; echo "EXPORTS"; printf '%s\n' GetModuleFileNameW CreateProcessW WaitForSingleObject \
	GetExitCodeProcess GetFileAttributesW CreateFileW ReadFile WriteFile CloseHandle GetFileSize MoveFileExW DeleteFileW \
	CreateDirectoryW GetTickCount ExitProcess; } > "$tmp/kernel32.def"
printf 'LIBRARY urlmon.dll\nEXPORTS\nURLDownloadToFileW\n' > "$tmp/urlmon.def"
printf 'LIBRARY user32.dll\nEXPORTS\nMessageBoxW\n' > "$tmp/user32.def"
llvm-dlltool -m i386:x86-64 -d "$tmp/kernel32.def" -l "$tmp/kernel32.lib"
llvm-dlltool -m i386:x86-64 -d "$tmp/user32.def" -l "$tmp/user32.lib"
llvm-dlltool -m i386:x86-64 -d "$tmp/urlmon.def" -l "$tmp/urlmon.lib"
clang --target=x86_64-pc-windows-msvc -O2 -ffreestanding -fno-stack-protector -fshort-wchar -mno-stack-arg-probe \
	-c "$here/SkillingHelper.c" -o "$tmp/launcher.obj"
lld-link /nologo /entry:start /subsystem:windows /nodefaultlib /out:"$out" \
	"$tmp/launcher.obj" "$tmp/kernel32.lib" "$tmp/user32.lib" "$tmp/urlmon.lib"
echo "Built $out"
