## !!!Be aware of scam!!! The sites like https :// fermata-auto . com  are 100% scam. This app is free and opensource!


## Stato del progetto – branch `kodi-backend-prototype`

Questo è il ramo di lavoro per l'integrazione di Kodi come backend locale di Fermata. Il ramo `main` e il ramo `fermata-kodi-auto-clean` non fanno parte delle modifiche correnti.

### Base di lavoro confermata
- Base: **Build APK #138**, versione `2.0.2-Kodi-0.1.23`, versionCode `292`.
- Commit di riferimento: `f7b00032ca60d1b2276b372638ccfe24bf0de36c`.
- Ramo operativo: `kodi-backend-prototype`.
- L'utente ha confermato il funzionamento sul dispositivo. Le prossime attività devono partire da questo stato, senza ripristinare vecchi tentativi o modificare `main` e `fermata-kodi-auto-clean`.

### Obiettivo e vincoli
- Mantenere l'interfaccia moderna già presente in Fermata.
- Fermata resta l'interfaccia/player Android Auto; Kodi resta un'app separata sullo stesso telefono.
- Non incorporare l'interfaccia completa di Kodi e non duplicare i servizi Android Auto.
- Le modifiche alla presentazione Android Auto devono restare separate e verificabili rispetto alla UI moderna del telefono.
- La riproduzione video in Android Auto è destinata all'uso da fermo o al passeggero.

### Completato nel codice
- Aggiunte nella schermata moderna **Impostazioni > Informazioni** le informazioni di versione e numero build.
- Aggiunta la sezione **Kodi** nelle impostazioni moderne con il comando **Verifica collegamento**.
- Il test interroga il servizio JSON-RPC locale di Kodi all'indirizzo `http://127.0.0.1:8080/jsonrpc`, usando inizialmente la configurazione senza autenticazione.
- Aggiunto **Impostazioni > Kodi > Log diagnosi**: registro persistente e limitato degli eventi JSON-RPC, tempi di risposta ed errori; il log può essere copiato o svuotato.
- Il log non registra il contenuto delle richieste, le password o le risposte complete di Kodi.
- Aggiunta nella schermata moderna una sezione **Collegamenti Kodi** con accessi diretti a **TV in diretta**, **Film**, **Serie TV**, **Preferiti** e **Playlist** di Kodi. I collegamenti avviano Kodi e selezionano la sezione tramite `GUI.ActivateWindow` su JSON-RPC; non creano sorgenti nella libreria tradizionale di Fermata.
- Integrata **Cartella Kodi** nell'albero MediaBrowser originale di Fermata, come unico punto d'ingresso Kodi nella schermata principale; rimossi i collegamenti Kodi duplicati al livello root.
- Le categorie **Film Kodi** e **Telefilm Kodi** sono ora nodi `BROWSABLE`: interrogano `VideoLibrary.GetMovies` e `VideoLibrary.GetTVShows` su JSON-RPC e mostrano i contenuti effettivi della libreria.
- Le serie TV aprono un secondo livello con gli episodi ottenuti tramite `VideoLibrary.GetEpisodes`.
- Le voci film/episodio sono foglie selezionabili; la selezione invia `Player.Open` con `movieid` o `episodeid` a Kodi, che resta l'app player separata. Non viene creato un player Fermata fittizio.
- Questa prima fase di browsing espone soltanto Film e Telefilm. Le categorie TV in diretta, Preferiti e Playlist non sono incluse nella Cartella Kodi di questa build: saranno aggiunte dopo aver implementato e verificato i rispettivi metodi JSON-RPC.

### Interfaccia moderna – finestre blu
- Le finestre delle sezioni Impostazioni, Dimensione interfaccia, Adattamento automatico, Riproduzione, Kodi, Credenziali, Diagnostica, Verifica collegamento e Informazioni condividono il pannello blu e i colori testuali/accento del menu Kodi.

### Browsing Kodi – TV, Preferiti e Playlist
- TV in diretta carica i canali del primo gruppo PVR TV restituito da Kodi; la selezione invia `Player.Open` con `channelid`.
- Preferiti carica le voci tramite `Favourites.GetFavourites`; apre i preferiti di tipo media o window.
- Playlist carica i file di `special://videoplaylists/` tramite `Files.GetDirectory`; la selezione apre la playlist in Kodi.
- Film e Serie TV, ordine Android Auto e player Fermata restano invariati. Verifica runtime ancora da effettuare con PVR, preferiti e playlist presenti in Kodi.

### Build APK #138 – base stabile verificata
- Versione `2.0.2-Kodi-0.1.23`, code `292`.
- Corretto il menu **Impostazioni > Kodi** della schermata moderna: la Build 72 combinava `AlertDialog.setMessage()` e `setItems()`; sul dispositivo dell'utente appariva soltanto il messaggio e non le tre azioni.
- Sostituito l'elenco del dialogo con una vista personalizzata contenente tre pulsanti espliciti: **Credenziali Kodi**, **Verifica collegamento** e **Log diagnosi**.
- Le tre azioni richiamano i metodi già esistenti in `HomeActivity`; nessuna modifica all'albero o all'ordine Android Auto.
- GitHub Actions: **completata con successo** ([Build APK #138](https://github.com/Diego83Roma/Kodi_Auto_Fermata/actions/runs/44158711367)).
- Verifica sul dispositivo confermata dall'utente: la schermata **Impostazioni > Kodi** mostra correttamente le azioni e ora funziona.
- Questa è la **base di partenza stabile** per gli sviluppi successivi. Mantenere invariati i comportamenti già verificati; introdurre le prossime modifiche in modo incrementale e controllato.

### Build 72 – credenziali nella schermata moderna
- Versione `2.0.2-Kodi-0.1.22`, code `291`.
- Corretto il punto di inserimento: il pulsante **Credenziali Kodi** è ora nella schermata moderna **Impostazioni > Kodi** di `HomeActivity`, nello stesso elenco di **Verifica collegamento** e **Log diagnosi**.
- La Build 71 aveva modificato soltanto `SettingsFragment`, cioè il pannello impostazioni tradizionale di Fermata: per questo il comando non compariva nella schermata moderna usata dall'utente. La modifica precedente era quindi nel posto sbagliato.
- La finestra moderna salva nome utente e password tramite `KodiSettings`; la password resta cifrata con Android Keystore. Anche il test e l'apertura delle sezioni Kodi usano ora le credenziali salvate.
- Nessuna modifica all'albero o all'ordine delle voci Android Auto. Build Actions e prova sul telefono ancora da verificare.

### Build 71 – tentativo nel pannello impostazioni tradizionale (posizione errata)
- Versione `2.0.2-Kodi-0.1.21`, code `290`.
- La modifica era stata inserita nella sottosezione Kodi di `SettingsFragment`, che non è la schermata moderna `HomeActivity`; il pulsante quindi non compariva nell'interfaccia moderna. Corretto nella Build 72.
- Il pulsante apre la finestra già esistente con i campi **Nome utente Kodi** e **Password Kodi**; il salvataggio continua a usare `KodiSettings` e la cifratura Android Keystore.
- Rimossi i riferimenti alle etichette del collegamento generale aggiunte per errore nella Build 69/70. Nessuna modifica alla schermata Android Auto o alle altre categorie.
- Controlli statici e compilazione GitHub Actions da verificare; test sul telefono richiesto.

### Build 69 – accesso diretto alle credenziali Kodi
- Versione `2.0.2-Kodi-0.1.19`, code `288`.
- Spostato l'accesso **Inserisci username e password Kodi** direttamente nella schermata principale di **Impostazioni**, come pulsante evidente, invece di lasciarlo solo dentro la sottosezione Kodi.
- Il pulsante apre esplicitamente la finestra con i campi **Nome utente Kodi** e **Password Kodi** e salva tramite `KodiSettings` (password cifrata con Android Keystore).
- La sottosezione **Kodi** mantiene la verifica del collegamento. Nessuna modifica alla struttura Android Auto o alle altre categorie.
- La compilazione GitHub Actions e il test sul telefono sono in attesa di verifica.

### Build 68 – correzione traduzione italiana
- Versione `2.0.2-Kodi-0.1.18`, code `287`.
- Corretta la traduzione italiana della voce delle credenziali: il testo localizzato prevaleva su quello predefinito e continuava a mostrare “Configura collegamento”. Ora la voce è “Credenziali Kodi” e apre la finestra con i due campi.
- Nessuna modifica alla struttura Android Auto.
- Compilazione e test su dispositivo da verificare.

### Build 67 – credenziali Kodi nelle impostazioni Fermata Auto
- Versione `2.0.2-Kodi-0.1.17`, code `286`.
- La sezione **Impostazioni > Kodi** dell'app sul telefono espone **Credenziali Kodi**: apre i campi **Nome utente Kodi** e **Password Kodi**.
- Il nome utente viene salvato nelle preferenze private; la password viene cifrata con una chiave AES/GCM conservata in Android Keystore. I campi vuoti rimuovono le credenziali salvate e sono adatti quando l'autenticazione HTTP di Kodi è disattivata.
- La voce **Verifica collegamento** resta disponibile per controllare JSON-RPC dopo il salvataggio. Le credenziali non vengono mostrate né richieste sul display Android Auto.
- Questa build non modifica l'ordine o le categorie della schermata Android Auto.
- Compilazione GitHub Actions e prova su dispositivo da verificare.

### Build 66 – verifica collegamento da Android Auto (in compilazione)
- Versione `2.0.2-Kodi-0.1.16`, code `285`.
- Aggiunta in **Cartella Kodi** la voce **Collegamento Kodi**, con l'azione **Verifica collegamento Kodi**.
- L'azione interroga `JSONRPC.Version` sull'endpoint locale configurato e mostra lo stato di connessione nella sessione multimediale Android Auto; in caso di errore registra la diagnosi e restituisce uno stato di errore.
- Non è stata aggiunta una maschera per username/password nell'auto: le credenziali restano configurabili dall'app sul telefono, come concordato per i limiti dell'interfaccia MediaBrowser.
- TV in diretta, Preferiti e Playlist continuano in questa build a usare i collegamenti alla sezione nativa Kodi; il caricamento delle rispettive liste reali è il passo successivo.
- Compilazione GitHub Actions e prova su dispositivo ancora da verificare.

### Compilazione e verifica
- Build 56 (`2.0.2-Kodi-0.1.7`, code `276`): verificata dall'utente sul dispositivo; tutti e cinque i collegamenti della schermata moderna aprono correttamente le rispettive sezioni Kodi.
- Build 42: **fallita** durante la compilazione per una doppia assegnazione potenziale in `HomeActivity.java`.
- Build 43: annullata perché il successivo push README ha avviato una nuova esecuzione.
- Build 44: [Action – Document current Kodi backend development progress](https://github.com/Diego83Roma/Kodi_Auto_Fermata/actions/runs/43842668751), **completata con successo**; APK `arm64` pubblicato.
- Test sul dispositivo confermato dall'utente: **il collegamento JSON-RPC locale con Kodi funziona**.
- Build 52 (`2.0.2-Kodi-0.1.6`, code `275`): i collegamenti Kodi sono presenti ma il test sul dispositivo ha evidenziato che `GUI.ActivateWindow` veniva interpretato erroneamente quando Kodi restituiva un risultato scalare (`"OK"`). Il ciclo di avvio ripeteva inoltre il comando di navigazione fino a 20 volte.
- Correzione inclusa nella build 56: il client accetta risultati JSON-RPC scalari, attende Kodi riprovando solo `JSONRPC.Version` e invia `GUI.ActivateWindow` una sola volta.
- Build 57 (`2.0.2-Kodi-0.1.8`, code `277`): compilazione fallita per il metodo `KodiShortcutItem.create()` non accessibile da `DefaultMediaLib` (visibilità package-private). Correzione inclusa nella build 58: metodo reso pubblico; versione `2.0.2-Kodi-0.1.9`, code `278`.
- Build 58: l'utente ha confermato che Android Auto mostra Cartella Kodi e che la selezione apre la sezione corretta sul telefono, ma Android Auto segnala **Impossibile caricare la selezione**. La diagnosi ha individuato che il callback apriva Kodi senza aggiornare metadata/stato della sessione multimediale.
- Build 59 (`2.0.2-Kodi-0.1.10`, code `279`): compilazione completata; l'utente ha installato la build e confermato il corretto funzionamento sul telefono. Prova Android Auto in auto pianificata.
- Build 60 (test grafico): versione `2.0.2-Kodi-0.1.11`, code `280`; ha provato i suggerimenti MediaBrowser a griglia. L'utente ha verificato che la differenza era minima.
- Build 61 (test elenco verticale): versione `2.0.2-Kodi-0.1.12`, code `281`; ripristina i suggerimenti MediaBrowser a elenco verticale. Porta TV Kodi e Playlist Kodi direttamente al livello principale insieme a Cartella Kodi, prima delle categorie standard. Aggiunge artwork alla sessione virtuale Kodi e registra il comando Play Android Auto; dopo una risposta JSON-RPC positiva ripristina lo stato virtuale in pausa. Migliora la presentazione ma non simula una riproduzione reale: durata e avanzamento restano assenti finché Kodi non viene integrato come sorgente di playback.

### Build 62 – regressione riscontrata
- Versione `2.0.2-Kodi-0.1.13`, code `282`; test utente negativo: le selezioni non aprivano più i collegamenti Kodi e l'ordine della root era alterato.
- La causa principale era il modello MediaBrowser: categorie e contenuti erano stati esposti con flag non coerenti e le azioni foglia non venivano instradate correttamente.

### Build 65 – ripristino funzioni Kodi e configurazione connessione (in verifica)
- Versione `2.0.2-Kodi-0.1.15`, code `284`.
- Mantiene un solo ingresso **Cartella Kodi** dopo le prime tre voci Fermata; non aggiunge collegamenti Kodi duplicati alla root.
- Tutte le cinque categorie originali sono presenti in **Cartella Kodi**: **TV in diretta**, **Film**, **Serie TV/Telefilm**, **Preferiti** e **Playlist**.
- Le categorie sono nodi MediaBrowser `BROWSABLE`; film e serie caricano la libreria con `VideoLibrary.GetMovies` e `VideoLibrary.GetTVShows`, e gli episodi con `VideoLibrary.GetEpisodes`.
- Film ed episodi sono elementi `PLAYABLE` e inviano `Player.Open` a Kodi. Play/Pausa/Stop della sessione Android Auto vengono inoltrati al player Kodi.
- TV in diretta, Preferiti e Playlist mantengono l'apertura della sezione nativa corrispondente nell'app Kodi.
- In **Impostazioni > Kodi** sono disponibili configurazione nome utente/password e verifica connessione. L'endpoint resta locale `127.0.0.1:8080`; la password viene cifrata con Android Keystore. Lasciare i campi vuoti se l'autenticazione HTTP di Kodi è disattivata.
- Tutte le chiamate JSON-RPC usano le credenziali salvate.
- Build e test sul dispositivo sono ancora da confermare; non considerare completata la verifica finché l'APK non è compilato e provato in auto.
### Build 63 – ripristino collegamenti Kodi (in compilazione)
- Versione prevista: `2.0.2-Kodi-0.1.14`, code `283`.
- Ripristinato il modello precedente delle cinque scorciatoie: TV in diretta, Film, Serie TV, Preferiti e Playlist.
- Ripristinata la Cartella Kodi con tutte e cinque le voci.
- Ripristinato l'ordine Android Auto precedente alla Build 62, con Cartella Kodi in fondo all'elenco invece che in testa.
- La compilazione e la prova sul dispositivo sono ancora da verificare.
- Il ripristino è intenzionalmente separato dal successivo lavoro di browsing: prima si verifica il ritorno dei collegamenti storici in auto, poi si implementa la libreria Kodi senza cambiare contemporaneamente il modello delle scorciatoie.

### Esito del test utente Build 59/60
- Build 59: in Android Auto le voci risultavano 3 più Altro; TV e Playlist Kodi restavano in Altro. La selezione Kodi apriva la sezione sul telefono, ma Android Auto mostrava artwork grigio, posizione 0 e Play senza effetto percepibile.
- Build 60: il suggerimento a griglia ha cambiato poco la presentazione.

### Limiti e prossimi passi
- I collegamenti aprono le sezioni nell’app Kodi separata: non incorporano Kodi nella schermata Fermata e non trasferiscono ancora la riproduzione al player Fermata.
- Il collegamento alle sezioni dipende dal servizio HTTP/JSON-RPC di Kodi attivo su `127.0.0.1:8080` e dai permessi di controllo GUI concessi a JSON-RPC.

## Fermata Media Player
[<img alt="Get it on Google Play" height="60" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png">](https://play.google.com/store/apps/details?id=me.aap.fermata)

## About
Fermata Media Player is a free, open source audio, video and TV player with a simple and intuitive interface. It is focused on playing media files organized in folders and playlists.

Supported features:

* Play media files organized in folders
* IPTV addon with support for XMLTV EPG and Catchup
* Remembers the last played track and position for each folder
* Support for favorites and playlists
* Support for CUE and M3U playlists
* Support for bookmarks
* Audio effects: Equalizer, Bass/Volume Boost and Virtualizer
* Configure audio effects for individual tracks and folders
* Configure playback speed for individual tracks and folders
* Customizable titles and subtitles
* Support for Android Auto
* Support for Android TV
* Show favorites and playlists on Android TV home screen
* Pluggable media engines: MediaPlayer, ExoPlayer and VLC
* Video player with support for subtitles and audio streams (VLC Engine only)

## Building the project
* Download and install the latest Android SDK or Android Studio from https://developer.android.com/studio/
* Set the environment variable ANDROID_SDK_ROOT pointing to the SDK directory
```bash
export ANDROID_SDK_ROOT=<path to android SDK>
```

### Clone the repository
```bash
git clone --recurse-submodules https://github.com/AndreyPavlenko/Fermata.git
cd Fermata
```

### Build AAB
```bash
./gradlew bundleAutoRelease -PAPP_ID_SFX=.type.your.pkg.sfx.here
find $PWD -name *.aab
```

### Build APK
```bash
./gradlew bundleAutoRelease -PAPP_ID_SFX=.type.your.pkg.sfx.here
find $PWD -name *.apk
```

### Building in docker
```bash
docker run -ti --name Fermata andreypavlenko/fermata
```
Enter the requested key alias and password, when prompted.
Build the required package using the above commands.
To copy the built package to the host machine, open a new terminal and run:
```bash
docker cp Fermata:/home/mobiledevops/Fermata/fermata/build/outputs/bundle/autoRelease/ .
```


## Donation
If you like the application, please consider making a donation:

[PayPal](https://www.paypal.com/donate/?hosted_button_id=NP5Q3YDSCJ98N)

[CloudTips](https://pay.cloudtips.ru/p/a03a73da)

[Yandex Money](https://money.yandex.ru/to/410014661137336)
