package com.example.copiadoapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.UUID
import kotlinx.coroutines.*

// Bits dos comandos
const val UP = 1
const val DOWN = 2
const val RIGHT = 4
const val LEFT = 8
const val ACTION = 16

var bluetoothSocket: BluetoothSocket? = null

class MainActivity : ComponentActivity() {

    private val bluetoothPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val permitido =
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true

            if (permitido) {
                BluetoothConect(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permissões
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            bluetoothPermission.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )

        } else {
            BluetoothConect(this)
        }

        setContent {
            ControleRobo()
        }
    }
}


// --------------------------------------------------
// CONEXÃO BLUETOOTH
// --------------------------------------------------

fun BluetoothConect(context: Context) {

    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    if (bluetoothAdapter == null) {
        println("Bluetooth não disponível")
        return
    }

    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val dispositivo =
        bluetoothAdapter.bondedDevices
            .firstOrNull {
                it.name == "Bluetooth_Robozao"
            }

    if (dispositivo == null) {
        println("ESP32 não encontrado")
        return
    }

    try {

        val uuid = UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )

        bluetoothSocket =
            dispositivo.createRfcommSocketToServiceRecord(uuid)

        bluetoothSocket?.connect()

        println("Bluetooth conectado!")

    } catch (e: Exception) {

        println("Erro Bluetooth: ${e.message}")

        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {
        }

        bluetoothSocket = null
    }
}


// --------------------------------------------------
// ENVIO
// --------------------------------------------------

fun sendCommand(comando: Int) {

    try {

        bluetoothSocket
            ?.outputStream
            ?.write(comando)

        println("Enviado: $comando")

    } catch (e: Exception) {

        println("Erro ao enviar: ${e.message}")
    }
}


// --------------------------------------------------
// BOTÃO
// --------------------------------------------------

@Composable
fun BotaoMovimento(
    texto: String,
    comando: Int,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.LightGray)
            .pointerInput(Unit) {

                detectTapGestures(

                    onPress = {

                        onPress()

                        try {
                            awaitRelease()
                        } finally {
                            onRelease()
                        }
                    }
                )
            },

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 30.sp
        )
    }
}


// --------------------------------------------------
// CONTROLE
// --------------------------------------------------

@Composable
fun ControleRobo() {

    var comandosAtivos by remember {
        mutableStateOf(0)
    }

    // Guarda a tarefa responsável pelo envio contínuo
    var envioJob by remember {
        mutableStateOf<Job?>(null)
    }

    fun pressionar(comando: Int) {

        // Adiciona o comando aos comandos ativos
        comandosAtivos =
            comandosAtivos or comando

        // Cria o envio contínuo
        if (envioJob == null) {

            envioJob = CoroutineScope(
                Dispatchers.IO
            ).launch {

                while (isActive) {

                    // Envia o comando atual
                    sendCommand(comandosAtivos)

                    // Espera 100 ms
                    delay(25)
                }
            }
        }
    }

    fun soltar(comando: Int) {

        // Remove o comando dos comandos ativos
        comandosAtivos =
            comandosAtivos and comando.inv()

        // Se nenhum botão estiver pressionado,
        // interrompe o envio contínuo
        if (comandosAtivos == 0) {

            envioJob?.cancel()
            envioJob = null

            // Envia 0 para o ESP32
            sendCommand(0)
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // UP
        BotaoMovimento(
            texto = "U",
            comando = UP,
            onPress = {
                pressionar(UP)
            },
            onRelease = {
                soltar(UP)
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )


        // ESQUERDA + AÇÃO + DIREITA
        Row {

            BotaoMovimento(
                texto = "L",
                comando = LEFT,
                onPress = {
                    pressionar(LEFT)
                },
                onRelease = {
                    soltar(LEFT)
                }
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            BotaoMovimento(
                texto = "A",
                comando = ACTION,
                onPress = {
                    pressionar(ACTION)
                },
                onRelease = {
                    soltar(ACTION)
                }
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            BotaoMovimento(
                texto = "R",
                comando = RIGHT,
                onPress = {
                    pressionar(RIGHT)
                },
                onRelease = {
                    soltar(RIGHT)
                }
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )


        // DOWN
        BotaoMovimento(
            texto = "D",
            comando = DOWN,
            onPress = {
                pressionar(DOWN)
            },
            onRelease = {
                soltar(DOWN)
            }
        )


        Spacer(
            modifier = Modifier.height(30.dp)
        )


        // Mostra o byte atual
        Text(
            text = "Comando: $comandosAtivos",
            fontSize = 20.sp
        )
    }
}
