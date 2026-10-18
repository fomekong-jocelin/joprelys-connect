<#
.SYNOPSIS
    Récupère le dernier code OTP reçu par le serveur mail local (MailDev).

.DESCRIPTION
    Interroge l'API REST de MailDev sur http://localhost:1080/api/email, affiche les détails
    du dernier e-mail transactionnel et extrait automatiquement le code OTP à 6 chiffres.
    MailDev renvoie les e-mails du plus ancien au plus récent : le script les trie par date.
    Seul le dernier code envoyé à un compte est valide (5 minutes, 3 essais).

.PARAMETER Email
    Adresse du compte : ne retient que les e-mails envoyés à cette adresse.

.EXAMPLE
    .\scripts\get-latest-otp.ps1 -Email qa.admin@joprelys.local
#>

param(
    [string]$Email
)

$webPort = 1080
$apiUrl = "http://localhost:$webPort/api/email"

try {
    $emails = Invoke-RestMethod -Uri $apiUrl -ErrorAction Stop
} catch {
    Write-Host "[ERREUR] Impossible de joindre le serveur mail sur $apiUrl." -ForegroundColor Red
    Write-Host "Assurez-vous que MailDev est démarré avec .\scripts\start-local-mail-server.ps1" -ForegroundColor Yellow
    exit 1
}

if (-not $emails -or $emails.Count -eq 0) {
    Write-Host "[INFO] Aucun email n'a encore été reçu par le serveur mail local." -ForegroundColor Yellow
    Write-Host "Effectuez une action (login, mot de passe oublié, etc.) sur l'application puis relancez ce script." -ForegroundColor Cyan
    exit 0
}

if ($Email) {
    $emails = @($emails | Where-Object { $_.to | Where-Object { $_.address -ieq $Email } })
    if ($emails.Count -eq 0) {
        Write-Host "[INFO] Aucun e-mail reçu pour $Email." -ForegroundColor Yellow
        exit 0
    }
}

# MailDev liste les e-mails du plus ancien au plus récent : on retient le plus récent.
$latest = $emails | Sort-Object { [datetime]$_.time } -Descending | Select-Object -First 1
$recipient = if ($latest.to -and $latest.to[0].address) { $latest.to[0].address } else { "Inconnu" }
$date = $latest.time

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   Dernier Email Reçu - Joprelys Connect" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Date/Heure   : $date"
Write-Host "Destinataire : $recipient"
Write-Host "Sujet        : $($latest.subject)"
Write-Host ""

$text = $latest.text
if ($text -match '\b(\d{6})\b') {
    $otp = $matches[1]
    Write-Host "----------------------------------------------------------" -ForegroundColor Green
    Write-Host " >>> CODE OTP DÉTECTÉ : $otp <<<" -ForegroundColor Green
    Write-Host "----------------------------------------------------------" -ForegroundColor Green
} else {
    Write-Host "[INFO] Aucun code à 6 chiffres détecté dans le corps du message." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Contenu texte :" -ForegroundColor Gray
Write-Host $text -ForegroundColor DarkGray
Write-Host ""
Write-Host "Pour voir la version HTML enrichie : http://localhost:$webPort/" -ForegroundColor Cyan
