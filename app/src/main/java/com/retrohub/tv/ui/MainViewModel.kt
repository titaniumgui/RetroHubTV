package com.retrohub.tv.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.retrohub.tv.RetroHubApp
import com.retrohub.tv.data.AppDatabase
import com.retrohub.tv.data.AppSettings
import com.retrohub.tv.data.GameEntity
import com.retrohub.tv.data.GameStatsEntity
import com.retrohub.tv.data.GameTile
import com.retrohub.tv.data.ProfileEntity
import com.retrohub.tv.data.SystemEntity
import com.retrohub.tv.data.SystemWithCount
import com.retrohub.tv.launch.LaunchPlanner
import com.retrohub.tv.launch.RetroArchInfo
import com.retrohub.tv.detect.Proposal
import com.retrohub.tv.detect.SafPaths
import com.retrohub.tv.data.CoreCatalog
import com.retrohub.tv.data.SystemPresets
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.saves.ProfileStorage
import com.retrohub.tv.saves.SaveStateInfo
import com.retrohub.tv.saves.SaveStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RetroHubApp
    private val db = app.database
    private val store = app.settingsStore

    val isLite get() = app.isLite
    val memory get() = app.memory

    // ---------------- navigation ----------------
    val stack = mutableStateListOf<Screen>(Screen.Home)
    fun navigate(screen: Screen) { stack.add(screen) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun replaceTop(screen: Screen) { stack[stack.lastIndex] = screen }
    fun goHome() { while (stack.size > 1) stack.removeAt(stack.lastIndex) }
    /** Top-level tabs sit directly above Home, so Back always returns to the dashboard. */
    fun goTab(screen: Screen) { goHome(); if (screen != Screen.Home) stack.add(screen) }

    /** Last focused element per screen, so D-pad focus lands where the user left it. */
    val focusMemory = HashMap<String, String>()

    // ---------------- data ----------------
    private val started = SharingStarted.WhileSubscribed(5_000)

    val settings: StateFlow<AppSettings> = store.settings
    private val profileId: Flow<Long> = settings.map { it.currentProfileId }.distinctUntilChanged()

    val profiles: StateFlow<List<ProfileEntity>?> = db.profiles().observeAll().stateIn(viewModelScope, started, null)
    val currentProfile: StateFlow<ProfileEntity?> =
        combine(profiles, profileId) { list, id -> list?.firstOrNull { it.id == id } ?: list?.firstOrNull() }
            .stateIn(viewModelScope, started, null)

    // null = not loaded yet (screens wait before placing the initial D-pad focus).
    val systems: StateFlow<List<SystemWithCount>?> =
        db.systems().observeWithCounts().stateIn(viewModelScope, started, null)
    val systemNames: StateFlow<Map<Long, String>> =
        systems.map { list -> list.orEmpty().associate { it.system.id to it.system.name } }
            .stateIn(viewModelScope, started, emptyMap())
    val recent: StateFlow<List<GameTile>?> =
        profileId.flatMapLatest { db.games().observeRecent(it, RECENT_LIMIT) }.stateIn(viewModelScope, started, null)
    val favorites: StateFlow<List<GameTile>?> =
        profileId.flatMapLatest { db.games().observeFavorites(it, ROW_LIMIT) }.stateIn(viewModelScope, started, null)
    val gameCount: StateFlow<Int> = db.games().observeCount().stateIn(viewModelScope, started, 0)

    fun games(systemId: Long?, favoritesOnly: Boolean): Flow<List<GameTile>> = profileId.flatMapLatest { p ->
        when {
            systemId == null && favoritesOnly -> db.games().observeAllFavorites(p)
            systemId == null -> db.games().observeAll(p)
            favoritesOnly -> db.games().observeFavoritesBySystem(p, systemId)
            else -> db.games().observeBySystem(p, systemId)
        }
    }

    fun search(query: String): Flow<List<GameTile>> {
        val q = query.trim()
        if (q.isEmpty()) return flowOf(emptyList())
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return profileId.flatMapLatest { db.games().search(it, "%$escaped%", "$escaped%", SEARCH_LIMIT) }
    }

    fun game(id: Long): Flow<GameEntity?> = db.games().observe(id)
    fun stats(gameId: Long): Flow<GameStatsEntity?> = profileId.flatMapLatest { db.games().observeStats(it, gameId) }
    fun system(id: Long): Flow<SystemEntity?> = db.systems().observe(id)
    suspend fun loadSystem(id: Long): SystemEntity? = db.systems().byId(id)

    // ---------------- messages & background work ----------------
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private var messageJob: Job? = null

    fun toast(text: String) {
        _message.value = text
        messageJob?.cancel()
        messageJob = viewModelScope.launch { delay(5_000); _message.value = null }
    }

    /** Progress text for scans and metadata downloads (null = idle). */
    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()
    private var workJob: Job? = null

    fun scan(systemId: Long?) {
        if (workJob?.isActive == true) { toast("Already working — please wait"); return }
        workJob = viewModelScope.launch {
            val targets = if (systemId == null) db.systems().all() else listOfNotNull(db.systems().byId(systemId))
            if (targets.isEmpty()) { toast("Add a system first"); return@launch }
            val summary = StringBuilder()
            for (s in targets) {
                _busy.value = "Scanning ${s.name}…"
                val r = app.scanner.scan(s) { n -> _busy.value = "Scanning ${s.name}…  $n found" }
                if (summary.isNotEmpty()) summary.append("  •  ")
                summary.append(
                    if (r.error != null) "${r.systemName}: ${r.error}"
                    else "${r.systemName}: ${r.found} games" + if (r.removed > 0) " (${r.removed} removed)" else ""
                )
            }
            toast(summary.toString())
            if (settings.value.metadataAuto) runMetadata(targets.map { it.id }, retry = false, quiet = true)
            _busy.value = null
        }
    }

    fun fetchMetadata(retryNotFound: Boolean) {
        if (workJob?.isActive == true) { toast("Already working — please wait"); return }
        workJob = viewModelScope.launch {
            runMetadata(null, retryNotFound, quiet = false)
            _busy.value = null
        }
    }

    private suspend fun runMetadata(systemIds: List<Long>?, retry: Boolean, quiet: Boolean) {
        _busy.value = "Getting game info…"
        val r = app.metadata.run(systemIds, retry, settings.value.metadataSnaps) { _busy.value = it }
        val text = buildString {
            if (r.matched > 0 || r.notFound > 0) append("Game info: ${r.matched} matched")
            if (r.notFound > 0) append(", ${r.notFound} not in database")
            r.errors.forEach { if (isNotEmpty()) append("  •  "); append(it) }
        }
        if (text.isNotEmpty()) toast(text) else if (!quiet) toast("Game info is up to date")
    }

    fun cancelWork() { workJob?.cancel(); _busy.value = null }

    fun artworkBytes(): Long = app.metadata.artworkBytes()
    fun clearArtwork() = viewModelScope.launch {
        withContext(Dispatchers.IO) { app.metadata.clearArtwork() }
        db.games().clearDownloadedArtwork(app.filesDir.absolutePath + "/art/%")
        toast("Downloaded artwork removed. Use \"Download game info\" to fetch it again.")
    }

    // ---------------- games ----------------
    private suspend fun currentProfileId(): Long = settings.value.currentProfileId

    fun setFavorite(gameId: Long, favorite: Boolean) = viewModelScope.launch {
        val p = currentProfileId()
        db.withTransaction { db.games().ensureStats(p, gameId); db.games().setFavorite(p, gameId, favorite) }
    }

    fun setCoreOverride(id: Long, core: String) = viewModelScope.launch { db.games().setCoreOverride(id, core.trim()) }
    fun clearRecent() = viewModelScope.launch { db.games().clearRecent(currentProfileId()); toast("Recently played cleared") }

    fun planFor(game: GameEntity, system: SystemEntity): LaunchPlanner.Result {
        val s = settings.value
        // For the preview, show the profile config path that will be used (it is written at launch).
        val config = if (s.profileSaves) {
            java.io.File(ProfileStorage.profileDir(s, s.currentProfileId), "retroarch.cfg").absolutePath
        } else s.configFile
        return LaunchPlanner.plan(app.launcher.inputs(game, system, s, config))
    }

    /** [fresh] = ignore the auto-save for this launch ("Start fresh"). */
    fun play(gameId: Long, fresh: Boolean = false) = viewModelScope.launch {
        val game = db.games().byId(gameId) ?: return@launch
        val system = db.systems().byId(game.systemId) ?: return@launch
        // RetroArch reinstalled as another build (e.g. 32-bit → 64-bit)? Pick it up before launching.
        if (app.launcher.info(settings.value.retroArchPackage) == null) autoDetectRetroArch()
        val s = settings.value
        val p = s.currentProfileId
        // Always write a launch config: it carries the exit-to-RetroHub settings (and per-profile saves).
        val config = withContext(Dispatchers.IO) {
            ProfileStorage.writeConfig(s, p, autoLoad = !fresh).getOrElse { e ->
                toast("Couldn't prepare RetroArch's settings (${e.message}). Starting with RetroArch's own config.")
                s.configFile
            }
        }
        // RetroArch's Google Play edition has no storage permission: it can only open files through
        // folders it has been given, so the game must be passed in RetroArch's saf:// form.
        val launchGame = withContext(Dispatchers.IO) {
            if (!app.retroArchStorage.needsFolderGrants(s.retroArchPackage)) return@withContext game
            val saf = game.path?.let { SafPaths.forPath(it, app.retroArchStorage.knownFolders(s.retroArchPackage)) }
            saf?.let { game.copy(path = it) }
        }
        if (launchGame == null) {
            val folder = game.path?.substringBeforeLast('/') ?: "this game's folder"
            toast(
                "RetroArch can't open $folder yet. In RetroArch: Load Content → open that folder and start any game " +
                    "from it once. RetroHub then launches all games there automatically."
            )
            return@launch
        }
        val result = withContext(Dispatchers.Default) { LaunchPlanner.plan(app.launcher.inputs(launchGame, system, s, config)) }
        when (result) {
            is LaunchPlanner.Result.Blocked -> toast(result.reason)
            is LaunchPlanner.Result.Ready -> {
                if (s.clearTaskOnLaunch) {
                    // Start RetroArch fresh: a still-running RetroArch shows a black screen on new content.
                    withContext(Dispatchers.IO) { app.launcher.stopIfRunning(result.plan.packageName) }
                    delay(450)
                }
                val error = app.launcher.launch(result.plan, s.clearTaskOnLaunch)
                if (error != null) {
                    toast(error)
                } else {
                    db.withTransaction {
                        db.games().ensureStats(p, game.id)
                        db.games().markPlayed(p, game.id, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    suspend fun saveStates(game: GameEntity): List<SaveStateInfo> = withContext(Dispatchers.IO) {
        val s = settings.value
        if (!s.profileSaves) emptyList() else SaveStates.find(ProfileStorage.statesDir(s, s.currentProfileId), game.fileName)
    }

    fun deleteStates(states: List<SaveStateInfo>, onDone: () -> Unit) = viewModelScope.launch {
        val n = withContext(Dispatchers.IO) { SaveStates.deleteAll(states) }
        toast("Deleted $n save-state files")
        onDone()
    }

    // ---------------- profiles ----------------
    fun selectProfile(id: Long) {
        store.update { it.copy(currentProfileId = id) }
        focusMemory.clear()
        goHome()
    }

    fun addProfile(name: String) = viewModelScope.launch {
        val count = db.profiles().all().size
        db.profiles().insert(ProfileEntity(name = name.trim().ifEmpty { "Player ${count + 1}" }, colorIndex = count))
    }

    fun renameProfile(p: ProfileEntity, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) db.profiles().update(p.copy(name = name.trim()))
    }

    fun recolorProfile(p: ProfileEntity) = viewModelScope.launch { db.profiles().update(p.copy(colorIndex = p.colorIndex + 1)) }

    fun deleteProfile(p: ProfileEntity) = viewModelScope.launch {
        val all = db.profiles().all()
        if (all.size <= 1) { toast("Keep at least one profile"); return@launch }
        db.profiles().delete(p)
        if (settings.value.currentProfileId == p.id) {
            store.update { s -> s.copy(currentProfileId = all.first { it.id != p.id }.id) }
        }
        toast("Deleted ${p.name}. Their save files stay in the profile folder.")
    }

    // ---------------- systems ----------------
    fun saveSystem(system: SystemEntity, scanAfter: Boolean) = viewModelScope.launch {
        val named = system.copy(name = system.name.trim().ifEmpty { "Untitled" }, romFolder = system.romFolder.trim())
        // No core chosen? Pick the recommended one for this system automatically.
        val clean = if (named.coreName.isBlank() && named.corePath.isBlank()) {
            named.copy(coreName = CoreCatalog.choose(named.metadataDb, null).orEmpty())
        } else named
        val id = if (clean.id == 0L) db.systems().insert(clean) else { db.systems().update(clean); clean.id }
        back()
        toast("Saved ${clean.name}")
        if (scanAfter) scan(id)
    }

    fun deleteSystem(system: SystemEntity) = viewModelScope.launch {
        db.systems().delete(system)
        goHome()
        toast("Deleted ${system.name}")
    }

    // ---------------- settings & RetroArch ----------------
    fun updateSettings(transform: (AppSettings) -> AppSettings) = store.update(transform)

    fun retroArchInfo(): RetroArchInfo? = app.launcher.info(settings.value.retroArchPackage)

    /** Folders a Play-edition RetroArch can open (null when this RetroArch reads files normally). */
    fun retroArchFolderInfo(): List<String>? {
        val pkg = settings.value.retroArchPackage
        if (!app.retroArchStorage.needsFolderGrants(pkg)) return null
        return app.retroArchStorage.knownFolders(pkg).keys.map { it.removePrefix(StorageAccess.primaryRoot()).ifEmpty { "/" } }.sorted()
    }

    /** Every official RetroArch build installed on this TV. */
    fun installedRetroArch(): List<RetroArchInfo> = app.retroArchDetector.installed()

    /** "Detect RetroArch": fills package, core folder and config from what is installed. */
    fun detectRetroArch(preferPackage: String? = null) {
        val r = app.retroArchDetector.detect(settings.value, force = true, preferPackage = preferPackage)
        if (r.info == null) {
            toast("No RetroArch found. Install RetroArch (any official build) and try again.")
            return
        }
        store.update { r.settings }
        toast("Using ${r.info.packageName} ${r.info.versionName ?: ""}. Cores and config detected.")
    }

    /**
     * Gives every system without a core the right one automatically: the core RetroArch's own
     * playlists use for that system, else RetroHub's recommendation (CoreCatalog).
     */
    private suspend fun autoAssignCores() {
        val missing = db.systems().all().filter { it.coreName.isBlank() && it.corePath.isBlank() }
        if (missing.isEmpty()) return
        val s = settings.value
        val raCores = if (StorageAccess.hasBroadFileAccess(app)) {
            runCatching { app.discovery.retroArchCores(s.retroArchPackage, s.configFile) }.getOrDefault(emptyMap())
        } else emptyMap()
        var assigned = 0
        for (sys in missing) {
            val dbName = sys.metadataDb.ifBlank { SystemPresets.all.firstOrNull { it.name == sys.name }?.metadataDb.orEmpty() }
            val core = CoreCatalog.choose(dbName, raCores[dbName]) ?: continue
            db.systems().update(sys.copy(coreName = core, metadataDb = sys.metadataDb.ifBlank { dbName }))
            assigned++
        }
        if (assigned > 0) toast("Cores chosen automatically for $assigned system" + if (assigned == 1) "" else "s")
    }

    /** Silent check on start: fixes a missing/uninstalled RetroArch package and stale paths. */
    private fun autoDetectRetroArch() {
        val r = app.retroArchDetector.detect(settings.value, force = false)
        if (r.info != null && r.changes.isNotEmpty()) {
            store.update { r.settings }
            toast("Found RetroArch (${r.info.packageName}). Settings updated automatically.")
        }
    }

    // ---------------- automatic game discovery ----------------
    data class DiscoveryState(
        val running: Boolean,
        val progress: String? = null,
        val proposals: List<Proposal> = emptyList(),
        val playlistsFound: Int = 0,
        val done: Boolean = false,
    )

    private val _discovery = MutableStateFlow<DiscoveryState?>(null)
    val discovery: StateFlow<DiscoveryState?> = _discovery.asStateFlow()
    private var discoveryJob: Job? = null

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = viewModelScope.launch {
            _discovery.value = DiscoveryState(running = true, progress = "Starting…")
            val s = settings.value
            val result = app.discovery.run(s.retroArchPackage, s.configFile, db.systems().all()) { msg ->
                _discovery.value = _discovery.value?.copy(progress = msg)
            }
            _discovery.value = DiscoveryState(
                running = false,
                proposals = result.proposals,
                playlistsFound = result.playlistsFound,
                done = true,
            )
        }
    }

    fun cancelDiscovery() { discoveryJob?.cancel(); _discovery.value = null }

    /** Adds the chosen systems, then scans them (and downloads game info when enabled). */
    fun addProposals(chosen: List<Proposal>) = viewModelScope.launch {
        if (chosen.isEmpty()) { toast("Nothing selected"); return@launch }
        val order = db.systems().all().size
        chosen.forEachIndexed { i, p ->
            db.systems().insert(
                SystemEntity(
                    name = p.name,
                    romFolder = p.romFolder,
                    extensions = p.extensions.sorted().joinToString(","),
                    coreName = p.coreName,
                    metadataDb = p.metadataDb,
                    sortOrder = order + i,
                )
            )
        }
        _discovery.value = null
        goHome()
        toast("Added ${chosen.size} system" + (if (chosen.size == 1) "" else "s") + ". Scanning…")
        scan(null)
    }

    fun openRetroArch() { app.launcher.openRetroArch(settings.value.retroArchPackage)?.let(::toast) }

    init {
        autoDetectRetroArch()
        viewModelScope.launch { autoAssignCores() }
        viewModelScope.launch {
            val all = db.profiles().all()
            val s = settings.value
            if (all.isNotEmpty() && all.none { it.id == s.currentProfileId }) {
                store.update { it.copy(currentProfileId = all.first().id) }
            }
            if (s.askProfileOnStart && all.size > 1) navigate(Screen.Profiles())
        }
    }

    companion object {
        const val RECENT_LIMIT = 20
        const val ROW_LIMIT = 40
        const val SEARCH_LIMIT = 120
        val DEFAULT_PROFILE = AppDatabase.DEFAULT_PROFILE_ID
    }
}
