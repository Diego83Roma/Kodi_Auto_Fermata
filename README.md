## !!!Be aware of scam!!! The sites like https :// fermata-auto . com  are 100% scam. This app is free and opensource!


## Stato del progetto – branch `kodi-backend-prototype`

Questo è il ramo di lavoro per l'integrazione di Kodi come backend locale di Fermata. Il ramo `main` e il ramo `fermata-kodi-auto-clean` non fanno parte delle modifiche correnti.

### Obiettivo e vincoli
- Mantenere l'interfaccia moderna già presente in Fermata.
- Fermata resta l'interfaccia/player Android Auto; Kodi resta un'app separata sullo stesso telefono.
- Non incorporare l'interfaccia completa di Kodi e non duplicare i servizi Android Auto.
- Non modificare il design dell'interfaccia senza una richiesta esplicita.
- La riproduzione video in Android Auto è destinata all'uso da fermo o al passeggero.

### Completato nel codice
- Aggiunte nella schermata moderna **Impostazioni > Informazioni** le informazioni di versione e numero build.
- Aggiunta la sezione **Kodi** nelle impostazioni moderne con il comando **Verifica collegamento**.
- Il test interroga il servizio JSON-RPC locale di Kodi all'indirizzo `http://127.0.0.1:8080/jsonrpc`, usando inizialmente la configurazione senza autenticazione.
- Aggiunto **Impostazioni > Kodi > Log diagnosi**: registro persistente e limitato degli eventi JSON-RPC, tempi di risposta ed errori; il log può essere copiato o svuotato.
- Il log non registra il contenuto delle richieste, le password o le risposte complete di Kodi.
- Integrata la sorgente **Kodi** nella libreria media di Fermata: cartelle **Film** e **Serie TV**, elenco dei titoli e navigazione degli episodi tramite `VideoLibrary.GetMovies`, `VideoLibrary.GetTVShows` e `VideoLibrary.GetEpisodes`.

### Compilazione e verifica
- Build 42: **fallita** durante la compilazione per una doppia assegnazione potenziale in `HomeActivity.java`.
- Build 43: annullata perché il successivo push README ha avviato una nuova esecuzione.
- Build 44: [Action – Document current Kodi backend development progress](https://github.com/Diego83Roma/Kodi_Auto_Fermata/actions/runs/43842668751), **completata con successo**; APK `arm64` pubblicato.
- Test sul dispositivo confermato dall'utente: **il collegamento JSON-RPC locale con Kodi funziona**.
- Versione in preparazione: `2.0.2-Kodi-0.1.5`, build code `274`; contiene il log diagnostico e il catalogo Kodi navigabile.

### Limiti e prossimi passi
- In questa versione il catalogo Film/Serie TV è navigabile ma non avvia ancora la riproduzione in Fermata: la risoluzione dei percorsi Kodi `/vfs/` e il passaggio al player Fermata devono essere implementati e verificati separatamente.
- Gli Add-on Kodi restano una fase successiva, dopo la verifica del catalogo e della riproduzione dei file della libreria.

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
