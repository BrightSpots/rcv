:: idempotent-hash.bat
@echo off

:: NOTE: This script must be placed one level up from the rcv directory

echo Initiating batch hash procedure…
echo %date% %time%

setlocal EnableExtensions EnableDelayedExpansion

set "HASHFILE=all_hashes.txt"
set "TEMPHASHFILE=all_hashes_temp.txt"
set "EXTRACTIONDIR=.\rcv\modules_extracted"
set "MODULESFILE=.\rcv\lib\modules"

if exist %HASHFILE% (
	echo Deleting existing hash file, %HASHFILE% ...
    del %HASHFILE%
)

if exist %EXTRACTIONDIR% (
	echo Deleting existing extracted modules directory, %EXTRACTIONDIR% ...
    rmdir /s /q %EXTRACTIONDIR%
)

echo Extracting contents of modules file...
mkdir %EXTRACTIONDIR%
cd %EXTRACTIONDIR%
jimage extract ..\..\%MODULESFILE%

echo Temporarily relocating modules file...
cd ..\..
copy %MODULESFILE% .
del %MODULESFILE%

:: Calculate the hash for every file here and in all subdirectories, appending to the file (format "(filename) = (hash)")
echo Calculating hashes...
for /r .\rcv %%f in (*) do (
    <NUL set /p ="%%f = " >> %HASHFILE%
    C:\Windows\System32\certutil.exe -hashfile "%%f" SHA512 | findstr /v ":" >> %HASHFILE%
)

echo Restoring modules file...
move .\modules %MODULESFILE%

:: Replace the absolute paths to each file with relative paths (e.g. C:\temp\rcv => .\rcv)

echo Replacing absolute paths with relative paths in hash file...
set "SEARCHTEXT=%cd%"
set "REPLACETEXT=."
for /f "delims=" %%A in ('type "%HASHFILE%"') do (
    set "string=%%A"
    set "modified=!string:%SEARCHTEXT%=%REPLACETEXT%!"
    echo !modified!>>"%TEMPHASHFILE%"
)
del "%HASHFILE%"
rename "%TEMPHASHFILE%" "%HASHFILE%"

echo Sorting the hash file...
sort "%HASHFILE%" > "%TEMPHASHFILE%"
del "%HASHFILE%"
rename "%TEMPHASHFILE%" "%HASHFILE%"

echo Calculating the hash of the entire sorted hash file...
C:\Windows\System32\certutil.exe -hashfile %HASHFILE% SHA512

endlocal
