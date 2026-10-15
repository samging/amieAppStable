package org.example.project.components.render.templates

import org.example.project.components.terminal.window.controller.appLogFile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
//import org.example.project.components.ui.viewport.DevicePanel
import org.example.project.components.ui.viewport.DevicePanelK
import org.example.project.components.ui.viewport.NavigationSidebar
import org.example.project.components.ui.viewport.WindowHeader
import org.example.project.components.ui.viewport.LocalAppChrome
import androidx.compose.runtime.CompositionLocalProvider
import org.example.project.data.remote.parser.DeviceManager
import org.example.project.data.remote.parser.DeviceManagerFactory
import okio.Path.Companion.toPath
import org.example.project.components.system.config.handler.IndexedSelectionList
import org.example.project.components.system.config.handler.PeripheralPanel
import org.example.project.components.system.config.handler.RemotePackageConsole
import org.example.project.components.system.config.handler.SystemCommit
import org.example.project.components.terminal.window.controller.ConfigurationPanel
import org.example.project.components.terminal.window.controller.ConnectionPanel
import org.example.project.components.terminal.window.controller.ManageablePage
import org.example.project.components.ui.viewport.DataEntryList
import org.example.project.components.ui.viewport.EndpointEntryList
import org.example.project.getDirectory
import org.example.project.ui.theme.*
import org.example.project.ui.theme.UnifiedBodyBackground
import java.io.File
import org.example.project.components.terminal.window.controller.CodingSandbox

@Composable
fun AppBaseTemplate() {
    val okioPath = getDirectory()
    val appDir = File(okioPath.toString())
    if (!appDir.exists()) appDir.mkdirs()

    val navController = rememberNavController()
    // Who is signed in with Keycloak, or a guest name chosen on the login page
    val session by org.example.project.auth.AuthState.session.collectAsState()
    var guestUser by remember { mutableStateOf("") }
    val currentUser = session?.username ?: guestUser
    val scope = rememberCoroutineScope()

    var activePortsMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val configFile = remember { File(appDir, "componentSettings.json") }
    val configReader: DeviceManager = remember { DeviceManagerFactory.create(configFile) }
    // Devices are stored on the backend per user: follow the Keycloak session (a guest syncs as the guest id)
    androidx.compose.runtime.LaunchedEffect(session?.username) {
        session?.username?.let { configReader.setSession(it) }
    }
    val logFile = remember { appLogFile }
    if (!logFile.exists()) { logFile.writeText("System initialized\nWaiting for logs...\n") }


    // Models that were listening when the app closed start listening again; the saved session signs back in
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { org.example.project.auth.AuthState.restore() }
            org.example.project.components.terminal.window.controller.modules.ModelServer.restore()
        }
        // Database connections a script left open are closed when the app exits
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { org.example.project.components.terminal.window.controller.databases.Databases.closeAll() } })
    }
    val restoring by org.example.project.auth.AuthState.restoring.collectAsState()

    val windowChrome = LocalAppChrome.current
    val chrome = windowChrome.copy(
        user = currentUser,
        onLogin = { navController.navigate("login") { launchSingleTop = true } },
        onLogout = {
            guestUser = ""
            scope.launch { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { org.example.project.auth.AuthState.signOut() } }
        },
    )

    CompositionLocalProvider(LocalAppChrome provides chrome) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "start") {

            // Nothing is drawn until the saved session has been checked: a valid one goes straight to the
            // dashboard, anything else to the welcome screen. Neither can be returned to with Back.
            composable("start") {
                Box(modifier = Modifier.fillMaxSize().background(UnifiedBodyBackground))
                LaunchedEffect(restoring) {
                    if (restoring) return@LaunchedEffect
                    val signedIn = org.example.project.auth.AuthState.signedIn
                    navController.navigate(if (signedIn) "home1/" else "welcome") { popUpTo("start") { inclusive = true } }
                }
            }

            composable("welcome") {
                Column(modifier = Modifier.fillMaxSize()) {
                    // No account chip or Log in button here: Start goes to the sandbox as a guest, Sign in is the small option
                    WindowHeader(name = "Welcome", showOnBack = false, showUser = false, onBack = {})
                    WelcomePage(
                        navController = navController,
                        onStart = {
                            guestUser = "guest-desktop-user"
                            navController.navigate("home1/") { popUpTo("welcome") { inclusive = true } }
                        },
                    )
                }
            }

            composable("login") {
                Column(modifier = Modifier.fillMaxSize()) {
                    WindowHeader(name = "Log in", showUser = false, onBack = { navController.popBackStack() })
                    LoginPage(
                        onLoginSuccess = { username ->
                            if (session == null) guestUser = username
                            navController.navigate("home1/") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onRegister = { navController.navigate("register") { launchSingleTop = true } }
                    )
                }
            }

            composable("register") {
                Column(modifier = Modifier.fillMaxSize()) {
                    WindowHeader(name = "Create account", showUser = false, onBack = { navController.popBackStack() })
                    RegisterPage(
                        onRegistered = {
                            navController.navigate("home1/") {
                                popUpTo("welcome") { inclusive = false }
                            }
                        },
                        onBackToLogin = { navController.navigate("login") { popUpTo("login") { inclusive = true } } }
                    )
                }
            }

            // Model statistics: resources, uptime and traffic of every model; only for a Keycloak user
            composable(route = "models") {
                Column(modifier = Modifier.fillMaxSize()) {
                    WindowHeader(name = "Models", showOnBack = true, onBack = { navController.popBackStack() })
                    org.example.project.components.terminal.window.controller.ModelsPage(navController = navController)
                }
            }

            // The app's settings: the UI scale
            composable(route = "settings") {
                Column(modifier = Modifier.fillMaxSize()) {
                    WindowHeader(name = "Settings", showOnBack = true, onBack = { navController.popBackStack() })
                    org.example.project.settings.SettingsPage(navController = navController)
                }
            }

            composable(route = "home1/") {
                var selectedIndex by remember { mutableStateOf(1) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate("codingSandbox")
                }

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(
                        name = "Dashboard",
                        showUser = true,
                        user = currentUser,
                        onBack = { /*root should have no pop stack*/ },
                        showOnBack = false,
                        addComponent = true,
                        addComponentNav = {
                            navController.navigate("addDevice/${null}")
                        })

                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationSidebar(
                            selectedIndex = selectedIndex,
                            onIndexSelected = { 
                                selectedIndex = it 
                                if (it == 1) navController.navigate("codingSandbox")
                            },
                            settingsSelected = false,
                            onSettings = { navController.navigate("settings") },
                        )
                    val configMap = configReader.configuredDevicesStatic
                    println("DEBUG ${configMap.size} | ${configMap}")

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(configMap.toList()) { (idKey, dev) ->
                            DevicePanelK(
                                name = dev.name,
                                deviceEdnpoint = dev.deviceEndpoint ?: "",
                                endPort = dev.port,
                                onManage = { navController.navigate("configure/$idKey") },
                                onConfigure = { navController.navigate("manage/$idKey") },
                                onConnectPage = { navController.navigate("connect/$idKey") },
                                onDisconnect = { navController.navigate("disconnect/$idKey") }
                            )
                        }
                    }
                    }
                }
            }

            composable(route = "codingSandbox") {
                Column(modifier = Modifier.fillMaxSize()) {
                    WindowHeader(name = "Sandbox", showOnBack = false, onBack = {})
                    CodingSandbox(navController = navController)
                }
            }

            composable(route = "addDevice/{idKey}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("idKey") ?: ""

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(
                        name = "Add Device",
                        onBack = { navController.popBackStack() })

                    Column(modifier = Modifier.padding(16.dp)) {
                        data class DeviceConnectionState(
                            val name: String = "",
                            val port: String = "",
                            val deviceEndpoint: String = ""
                        )

                        var connectionState by remember { mutableStateOf(DeviceConnectionState()) }
                        val spaceBy = 0
                        PeripheralPanel(
                            name = "Device Name",
                            valueOf = connectionState.name,
                            hideButton = true,
                            onValueChange = { connectionState = connectionState.copy(name = it)},
                            deviceManager = configReader
                        )

                        Spacer(modifier = Modifier.height(spaceBy.dp))

                        PeripheralPanel(
                            name = "Device Port",
                            valueOf = connectionState.port,
                            hideButton = true,
                            onValueChange = { connectionState = connectionState.copy(port = it) },
                            deviceManager = configReader
                        )

                        Spacer(modifier = Modifier.height(spaceBy.dp))

                        PeripheralPanel(
                            name = "Device Endpoint",
                            valueOf = connectionState.deviceEndpoint,
                            hideButton = true,
                            onValueChange = { connectionState = connectionState.copy(deviceEndpoint = it) },
                            deviceManager = configReader
                        )

                        Spacer(modifier = Modifier.height((spaceBy*2).dp))

                        SystemCommit(
                            indexDevice = configReader.generateAddId(),
                            modifier = Modifier.fillMaxWidth(),
                            keyValues = listOf("name", "port", "deviceEndpoint"),
                            valuesOf = listOf(
                                connectionState.name,
                                connectionState.port,
                                connectionState.deviceEndpoint
                            ),
                            deviceManager = configReader,
                            redirectOnOk = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("need_refresh", true)
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
            composable(route = "manage/{deviceId}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""

                val deviceNameList = configReader.parseConfigByTargetId("name", deviceId)
                val currentDeviceName = deviceNameList.firstOrNull() ?: "Unknown Device"


                val currentDevice = configReader.getDevice(deviceId)
                val manageContent = remember { mutableStateListOf("ID: $deviceId", "Name: $currentDeviceName", "output") }

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(
                        name = "Configure Device (ID: ${deviceId})",
                        onBack = { navController.popBackStack() }
                    )

                    ManageablePage(
                        name = currentDevice?.name ?: "Unknown Device",
                        deviceId = deviceId,

                        deviceName = currentDevice?.name ?: "N/A",
                        portNumber = currentDevice?.port?.toIntOrNull() ?: 0,
                        endPoint = currentDevice?.deviceEndpoint?.toIntOrNull() ?: 0,

                        content = manageContent,
                        configureEndpoint = { navController.navigate("scrollableEndpoint/$deviceId/$currentUser") },
                        configurePort = { navController.navigate("scrollablePort/$deviceId") },
                        configureName = { navController.navigate("scrollableDevName/$deviceId") },
                        configurePlugins = { navController.navigate("scrollableNamePlugins/$deviceId") }
                    )
                }
            }

            composable(route = "scrollableEndpoint/{idKey}/{username}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("idKey") ?: ""
                val endpoint = backStackEntry.arguments?.getString("username") ?: ""

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(
                        name = "Endpoint",
                        onBack = { navController.popBackStack() }
                    )
                    val currentDevice = configReader.parseConfigByTargetId("deviceEndpoint",deviceId)

                    Column(modifier = Modifier.padding(16.dp)) {
//                        PeripheralPanel(
//                            name="Endpoint Device",
//                            modifier = Modifier,
//                            customText = "Set",
//                            writeId = deviceId,
//                            keyQuery = "deviceEndpoint",
//                            deviceManager = configReader,
//                            username = endpoint
//                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text="Current: ${currentDevice}", color = FontColor, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        val getAllNames = configReader.parseConfig("deviceEndpoint")
                        val allNames: Map<Int, String> = getAllNames.withIndex().associate { it.index to it.value }

                        EndpointEntryList(name = "a", modifier = Modifier, deviceManager = configReader, activeFields = allNames, writeId = deviceId)
                    }
                }
            }
            composable(route = "disconnect/{idKey}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("idKey") ?: ""

                LaunchedEffect(deviceId) {
                    withContext(Dispatchers.IO) {
                        configReader.deleteById(deviceId)
                    }
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("need_refresh", true)
                    navController.popBackStack()
                }
            }
            composable(route = "scrollablePort/{idKey}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("idKey") ?: ""
                var selectedPortByList by remember { mutableStateOf("") }
                var selectedId by remember { mutableStateOf<Int?>(null) }

                val currentPortList = configReader.parseConfigByTargetId("port", deviceId)
                val savedPort = currentPortList.firstOrNull() ?: ""

                LaunchedEffect(activePortsMap, savedPort) {
                    if (savedPort.isNotEmpty() && activePortsMap.isNotEmpty() && selectedId == null) {
                        val matchingEntry = activePortsMap.entries.find { 
                            val systemName = it.value.split(":").firstOrNull()?.trim() ?: ""
                            systemName.equals(savedPort.trim(), ignoreCase = true)
                        }
                        if (matchingEntry != null) {
                            selectedId = matchingEntry.key
                            selectedPortByList = savedPort
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(name = "Serial Port", onBack = { navController.popBackStack() })
//
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        val currentPort = configReader.parseConfigByTargetId("port",deviceId)
//                        PeripheralPanel(
//                            name="Serial Port",
//                            modifier = Modifier,
//                            customText = "Set",
//                            writeId = deviceId,
//                            keyQuery = "port",
//                            deviceManager = configReader,
//                            valueOf = selectedPortByList
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Text("Data entry", color = Color(0xFF878e9c), fontSize = 12.sp)
                        //bp
                        val currentPort = configReader.parseConfigByTargetId("port",deviceId)
                        Text(text="Current port: ${currentPort.first()}", color = Color(FontColor.toArgb()), fontSize = 18.sp, modifier = Modifier.padding(start = 24.dp, top = 24.dp))
                        DataEntryList(
                            name = "a", 
                            modifier = Modifier, 
                            activeFields = activePortsMap, 
                            currentlyActive = listOfNotNull(selectedId),
                            onPortsLoaded = { activePortsMap = it },
                            onPortSelected = { id, name -> 
                                selectedPortByList = name
                                selectedId = id
                            },
                            deviceManager = configReader,
                            writeId = deviceId,
                            keyQuery = "port"
                        )
                }
            }
            composable(route = "scrollableDevName/{idKey}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("idKey") ?: ""

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(name = "Device Name", onBack = { navController.popBackStack() })
                    Column(modifier = Modifier.padding(16.dp)) {
                        PeripheralPanel(name="Device Name", modifier = Modifier, customText = "Set", writeId = deviceId, keyQuery = "name", deviceManager = configReader)
                    }
                }
            }

            composable(route = "scrollableNamePlugins/{idKey}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("idKey") ?: ""

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(name = "Plugins", onBack = { navController.popBackStack() })
                    Column(modifier = Modifier.padding(16.dp)) {
                        RemotePackageConsole(name="Plugins", modifier = Modifier, customText = "Search")
                        Spacer(modifier = Modifier.height(16.dp))
                        IndexedSelectionList(name = "a", deviceId = deviceId, modifier = Modifier)
                    }
                }
            }

            composable(
                route = "configure/{deviceId}"
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(
                        name = "Terminal (ID: $deviceId)",
                        onBack = { navController.popBackStack() }
                    )
                    ConfigurationPanel(
                        deviceId = deviceId,
                        modifier = Modifier,
                        endPointNumber = 3,
                        allowCmd = true
                    )
                }
            }

            composable(
                route = "connect/{deviceId}"
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""

                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(
                        name = "Connect (ID: $deviceId)",
                        onBack = { navController.popBackStack() }
                    )
                    ConnectionPanel(
                        name = "android",
                        deviceId = deviceId,
                        endPoint = 3,
                        status = true,
                        connectionRedirect = { navController.navigate("changeEndpoint/$deviceId") }
                    )
                }
            }

            composable(route = "changeEndpoint/{deviceId}") { backStackEntry ->
                Column(modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()))) {
                    WindowHeader(name = "Select Endpoint", onBack = { navController.popBackStack() })
                    Column(modifier = Modifier.padding(16.dp)) {
                        PeripheralPanel(name="devices to toggle", modifier = Modifier, deviceManager = configReader)
                    }
                }
            }
        }
    }
    }
}
