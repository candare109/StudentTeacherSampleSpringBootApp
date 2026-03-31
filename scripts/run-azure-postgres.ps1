param(
    [Parameter(Mandatory = $true)]
    [string]$ServerName,

    [Parameter(Mandatory = $false)]
    [string]$DatabaseName = "studentdb",

    [Parameter(Mandatory = $true)]
    [string]$DbUsername,

    [Parameter(Mandatory = $true)]
    [string]$DbPassword,

    [switch]$UseServerQualifiedUsername
)

$javaExe = (Get-Command java).Source
$env:JAVA_HOME = Split-Path (Split-Path $javaExe -Parent) -Parent
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_URL = "jdbc:postgresql://$ServerName.postgres.database.azure.com:5432/$DatabaseName?sslmode=require"

if ($UseServerQualifiedUsername.IsPresent) {
    $env:DB_USERNAME = "$DbUsername@$ServerName"
} else {
    $env:DB_USERNAME = $DbUsername
}

$env:DB_PASSWORD = $DbPassword

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "SPRING_PROFILES_ACTIVE=$env:SPRING_PROFILES_ACTIVE"
Write-Host "DB_URL=$env:DB_URL"
Write-Host "DB_USERNAME=$env:DB_USERNAME"
Write-Host "Starting Spring Boot app..."

.\gradlew.bat bootRun

