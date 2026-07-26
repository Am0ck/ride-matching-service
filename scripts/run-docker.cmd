@echo off
setlocal

set "IMAGE_NAME=ride-matching-service:latest"
set "CONTAINER_NAME=ride-matching-service"
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"

echo Checking Docker...
where docker >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not installed or is not available on PATH. 1>&2
    endlocal
    exit /b 1
)

echo Removing an old container...
docker rm -f "%CONTAINER_NAME%" >nul 2>&1

pushd "%PROJECT_ROOT%" >nul
if errorlevel 1 (
    echo Error: Unable to access the project root "%PROJECT_ROOT%". 1>&2
    endlocal
    exit /b 1
)

echo Building the image...
docker build --tag "%IMAGE_NAME%" .
if errorlevel 1 (
    echo Error: Docker image build failed. 1>&2
    popd >nul
    endlocal
    exit /b 1
)

echo Starting the service...
echo Service URL: http://localhost:8080
echo Press Ctrl+C to stop the service.
docker run --rm --name "%CONTAINER_NAME%" -p "8080:8080" "%IMAGE_NAME%"
set "RUN_EXIT_CODE=%ERRORLEVEL%"

popd >nul
endlocal & exit /b %RUN_EXIT_CODE%
