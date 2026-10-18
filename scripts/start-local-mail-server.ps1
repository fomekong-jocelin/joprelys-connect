<#
.SYNOPSIS
    Démarre le serveur mail local (MailDev) pour intercepter les emails et codes OTP en développement local.

.DESCRIPTION
    Ce script vérifie la disponibilité des ports 1025 (SMTP) et 1080 (Web UI), puis démarre MailDev.
    L'interface Web est accessible à l'adresse http://localhost:1080/ pour visualiser les emails et codes OTP en temps réel.
#>

param(
    [switch]$Restart,
    [switch]$Stop
)

$smtpPort = 1025
$webPort = 1080

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   Joprelys Connect - Serveur Mail Local (MailDev)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if ($Restart -or $Stop) {
    Write-Host "[INFO] Arrêt des processus existants sur les ports $smtpPort / $webPort..." -ForegroundColor Yellow
    $activeConns = Get-NetTCPConnection -LocalPort $smtpPort, $webPort -ErrorAction SilentlyContinue
    $pidsToStop = $activeConns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -gt 0 }

    foreach ($procId in $pidsToStop) {
        try {
            $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($p) {
                Write-Host "  - Arrêt du processus $procId ($($p.ProcessName))..." -ForegroundColor DarkYellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        } catch {
            Write-Warning "Impossible d'arrêter le processus $procId : $_"
        }
    }
    Start-Sleep -Seconds 2
    if ($Stop) {
        Write-Host "[OK] Serveur mail local arrêté avec succès." -ForegroundColor Green
        exit 0
    }
}

$activeSmtp = Get-NetTCPConnection -LocalPort $smtpPort -ErrorAction SilentlyContinue
$activeWeb = Get-NetTCPConnection -LocalPort $webPort -ErrorAction SilentlyContinue

if ($activeSmtp -and $activeWeb) {
    Write-Host "[OK] Le serveur mail local est DÉJÀ EN COURS D'EXÉCUTION !" -ForegroundColor Green
    Write-Host "  - Port SMTP     : $smtpPort (localhost)" -ForegroundColor Yellow
    Write-Host "  - Interface Web : http://localhost:$webPort/" -ForegroundColor Yellow
    Write-Host "  - API REST      : http://localhost:$webPort/api/email" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Ouvrez http://localhost:$webPort/ dans votre navigateur pour consulter les emails et codes OTP." -ForegroundColor Cyan
    Write-Host "Astuce : utilisez -Restart pour forcer un redémarrage." -ForegroundColor DarkCyan
    exit 0
}

Write-Host "[INFO] Démarrage du serveur MailDev..." -ForegroundColor Yellow
Write-Host "  - Port SMTP     : $smtpPort"
Write-Host "  - Interface Web : http://localhost:$webPort/"
Write-Host ""

Start-Process -FilePath "npx.cmd" -ArgumentList "maildev", "--smtp", "$smtpPort", "--web", "$webPort", "--incoming-user", "noreply@joprelys.com", "--incoming-pass", "localdev" -WindowStyle Normal

Start-Sleep -Seconds 3

$checkWeb = Get-NetTCPConnection -LocalPort $webPort -ErrorAction SilentlyContinue
if ($checkWeb) {
    Write-Host "[SUCCÈS] Serveur mail local actif !" -ForegroundColor Green
    Write-Host "Consultez vos codes OTP à l'adresse : http://localhost:$webPort/" -ForegroundColor Green
} else {
    Write-Host "[ATTENTION] Vérifiez la fenêtre MailDev pour le statut de démarrage." -ForegroundColor Yellow
}
