@echo off
setlocal

REM ===== A adapter une seule fois =====
set REPO_URL=https://github.com/xav0688-rgb/MyPhotos.git
set BRANCH=main
REM =====================================

REM Initialise le repo git local si ce n'est pas deja fait
if not exist ".git" (
    git init
)

REM Ajoute le remote "origin" seulement s'il n'existe pas deja
git remote get-url origin >nul 2>&1
if errorlevel 1 (
    git remote add origin %REPO_URL%
)

git fetch origin

git add -A

set /p MSG="Message de commit : "
if "%MSG%"=="" set MSG=Mise a jour

git commit -m "%MSG%"

git branch -M %BRANCH%
git push -u origin %BRANCH% --force

echo.
echo Termine.
pause
