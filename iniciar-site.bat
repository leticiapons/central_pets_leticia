@echo off
title Central Pets - iniciar o site
cd /d "%~dp0"

echo.
echo ===== Central Pets: iniciar o site =====
echo.
echo 1) Encerrando programas antigos que estejam usando as portas 8080 e 3000...
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080,3000 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"
echo    Pronto.
echo.
set /p DB_PASSWORD=2) Digite a senha do PostgreSQL e aperte Enter: 
echo.
echo 3) Abrindo as janelas BACK-END e FRONT-END...
start "BACK-END" /D "%~dp0backend" cmd /k mvnw.cmd clean spring-boot:run
start "FRONT-END" /D "%~dp0." cmd /k npx.cmd serve frontend -l 3000
echo.
echo Aguarde aparecer "Started AtividadeOngsApplication" na janela BACK-END
echo (a primeira vez demora alguns minutos).
echo Depois abra http://localhost:3000 no navegador e aperte Ctrl + F5.
echo.
pause
