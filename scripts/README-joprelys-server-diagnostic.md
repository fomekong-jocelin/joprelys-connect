# Diagnostic serveur Joprelys depuis PowerShell

Le script cible par défaut `161.97.181.177` sur le port SSH `22` et n'affiche aucune invite.

```powershell
python -m pip install paramiko
$env:JOPRELYS_SSH_USER = "<utilisateur MobaXterm>"
$securePassword = Read-Host "Mot de passe SSH" -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
  $env:JOPRELYS_SSH_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
} finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}
python .\scripts\diagnose_joprelys_server.py
```

Au premier passage, le script s'arrête et affiche l'empreinte reçue. Comparez-la à l'empreinte de la session MobaXterm, puis relancez :

```powershell
$env:JOPRELYS_SSH_HOST_FINGERPRINT = "SHA256:<empreinte-validée>"
python .\scripts\diagnose_joprelys_server.py
```

Pour forcer explicitement le port déjà configuré dans MobaXterm :

```powershell
$env:JOPRELYS_SSH_HOST = "161.97.181.177"
$env:JOPRELYS_SSH_PORT = "22"
```

Après exécution, effacez le secret de la session PowerShell :

```powershell
Remove-Item Env:JOPRELYS_SSH_PASSWORD
```
