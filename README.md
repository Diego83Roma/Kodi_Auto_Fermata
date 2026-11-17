## !!!Be aware of scam!!! The sites like https :// fermata-auto . com  are 100% scam. This app is free and opensource!


## Stato del progetto – branch `kodi-backend-prototype`

Questo è il ramo di lavoro per l'integrazione di Kodi come backend locale di Fermata. Il ramo `main` e il ramo `fermata-kodi-auto-clean` non fanno parte delle modifiche correnti.

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

### Build 62 – browsing libreria Kodi (in verifica)
- Versione prevista: `2.0.2-Kodi-0.1.13`, code `282`.
- Corretto il modello MediaBrowser: Film e Telefilm sono cartelle sfogliabili, non elementi riproducibili virtuali.
- Aggiunti i modelli per film, serie ed episodi; la selezione di un film/episodio invia `Player.Open` al backend Kodi locale.
- Rimossi TV Kodi e Playlist Kodi duplicati dalla root; resta un solo punto d'ingresso Cartella Kodi.
- La compilazione e il test sul dispositivo non sono ancora confermati. Le categorie TV in diretta, Preferiti e Playlist sono temporaneamente escluse dalla Cartella Kodi e restano un lavoro successivo.

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
