:: sha-of-zip.bat
:: Windows port of sha-of-zip.sh. See that file for a description.
:: Usage: ./sha-of-zip.bat <ZIP_FILEPATH> <sha version: 1, 256, or 512>

@echo off

set ZIP_FILEPATH=%1
set SHA_A=%2

setlocal EnableExtensions EnableDelayedExpansion

:: All paths relative to pwd -- where this script is called from
set "EXTRACTIONDIR=.\rcv\zip_extracted"
set "MODULESFILE=.\rcv\zip_extracted\rcv\lib\modules"
set "MODULESDIR=.\rcv\zip_extracted\rcv\lib\modules_extracted"
set "HASHFILE_UNSORTED=all_hashes_unsorted.txt"
set "HASHFILE_PATH_STRIPPED=all_hashes_path_stripped.txt"
set "HASHFILE_SORTED=all_hashes_sorted.txt"

if exist %HASHFILE_UNSORTED% (
  del %HASHFILE_UNSORTED%
)

if exist %EXTRACTIONDIR% (
  rmdir /s /q %EXTRACTIONDIR%
)

if exist %MODULESDIR% (
  rmdir /s /q %EXTRACTIONDIR%
)
 
powershell -command Expand-Archive -Path %ZIP_FILEPATH% -Destination %EXTRACTIONDIR%

:: Extract modules, get the SHA-512 of it, and add it to the top of the hashfile
jimage extract --dir %MODULESDIR% %MODULESFILE%

powershell .github\actions\Sha-Of-Directory.ps1 %MODULESDIR% 512 > %HASHFILE_UNSORTED%

:: Delete both modules and the extracted dir
del %MODULESFILE%
rmdir /s /q %MODULESDIR%

:: Calculate the hash for every file here and in all subdirectories, appending to the file (format "(filename) = (hash)")
(
  for /r %EXTRACTIONDIR% %%f in (*) do (
      <NUL set /p ="%%f = "
      C:\Windows\System32\certutil.exe -hashfile "%%f" SHA%SHA_A% | findstr /v ":"
  )
) >> %HASHFILE_UNSORTED%

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

:: For debugging, enable printing the file-by-file hash
:: echo File-by-file hash
:: type "%HASHFILE_SORTED%"

endlocal
