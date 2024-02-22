:: sha-of-zip.bat
:: Windows port of sha-of-zip.sh. See that file for a description.
:: Usage: ./sha-of-zip.bat <ZIP_FILEPATH> <sha version: 1, 256, or 512>

@echo off

set ZIP_FILEPATH=%1
set SHA_A=%2

setlocal EnableExtensions EnableDelayedExpansion

set "HASHFILE_UNSORTED=all_hashes_unsorted.txt"
set "HASHFILE_PATH_STRIPPED=all_hashes_path_stripped.txt"
set "HASHFILE_SORTED=all_hashes_sorted.txt"
set "EXTRACTIONDIR=.\rcv\zip_extracted"

if exist %HASHFILE_UNSORTED% (
  del %HASHFILE_UNSORTED%
)

if exist %EXTRACTIONDIR% (
  rmdir /s /q %EXTRACTIONDIR%
)

mkdir %EXTRACTIONDIR%
powershell -command Expand-Archive -Path %ZIP_FILEPATH% -Destination %EXTRACTIONDIR%
cd %EXTRACTIONDIR%

:: Calculate the hash for every file here and in all subdirectories, appending to the file (format "(filename) = (hash)")
for /r . %%f in (*) do (
    <NUL set /p ="%%f = " >> %HASHFILE_UNSORTED%
    C:\Windows\System32\certutil.exe -hashfile "%%f" SHA%SHA_A% | findstr /v ":" >> %HASHFILE_UNSORTED%
)

:: Replace the absolute paths to each file with relative paths (e.g. C:\temp\rcv => .\rcv)
set "SEARCHTEXT=%cd%"
set "REPLACETEXT=."
for /f "delims=" %%A in ('type "%HASHFILE_UNSORTED%"') do (
    set "string=%%A"
    set "modified=!string:%SEARCHTEXT%=%REPLACETEXT%!"
    echo !modified!>>"%HASHFILE_PATH_STRIPPED%"
)

sort "%HASHFILE_PATH_STRIPPED%" > "%HASHFILE_SORTED%"

C:\Windows\System32\certutil.exe -hashfile %HASHFILE_SORTED% SHA%SHA_A% | findstr /v ":"

endlocal
