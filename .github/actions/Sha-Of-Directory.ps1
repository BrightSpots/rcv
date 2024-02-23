## Creates a SHA of all files in the directory but ignores their metadata
## This is useful when you want to ignore all timestamps of a zip, for example:
## You can extract the contents to a directory, then run this.
## Usage: powershell Sha-Of-Directory.ps1 <DIRECTORY> <sha version: 1, 256, or 512>

$Directory=$args[0]
$ShaA=$args[1]

# First, create a hash algorithm object using SHA256.
$Algorithm = [System.Security.Cryptography.HashAlgorithm]::Create("SHA" + $ShaA)

# Next, create a cryptographic stream of data using the SHA256 hash algorithm.
$CryptoStream = [System.Security.Cryptography.CryptoStream]::new(
    ([System.IO.Stream]::Null), 
    $Algorithm, 
    "Write"
)

# Retrieve each file and copy the data into the cryptographic stream.
echo $Directory
foreach ($File in Get-ChildItem -Recurse -Path $Directory -File) {
    # Write-Host $File
    $FileStream = [io.file]::OpenRead($File.FullName)
    $FileStream.CopyTo($CryptoStream)
}

# Close all files and close out the cryptographic stream.
$CryptoStream.FlushFinalBlock()

# Combine all of the hashes as hexadecimal formats "X2" and join the values.
($Algorithm.Hash | ForEach-Object {$_.ToString("X2")}) -join ''
