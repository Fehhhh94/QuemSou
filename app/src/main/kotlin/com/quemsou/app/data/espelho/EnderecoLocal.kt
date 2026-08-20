package com.quemsou.app.data.espelho

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Descobre o IPv4 do aparelho na rede local, para o espelho de leitura saber
 * qual endereço anunciar no QR.
 *
 * Usa [NetworkInterface] de propósito: é `java.net` puro e **não pede
 * permissão nenhuma** — `WifiManager` exigiria `ACCESS_WIFI_STATE` e ainda
 * assim erraria em roteamento por hotspot.
 *
 * Só interessam interfaces que os outros celulares da mesa conseguem
 * alcançar: Wi-Fi (`wlan…`), hotspot/tethering (`ap…`, `swlan…`) e Ethernet
 * (`eth…`, que é como o emulador aparece). Dados móveis (`rmnet…`, `ccmni…`),
 * VPN (`tun…`) e loopback ficam de fora — um IP desses no QR só geraria um
 * "não conecta" inexplicável.
 */
object EnderecoLocal {

    /**
     * IPv4 da rede local, ou `null` quando não há interface alcançável (sem
     * Wi-Fi) — a UI trata o nulo desligando o espelho e explicando.
     */
    fun descobrir(): String? = interfacesCandidatas()
        .sortedBy { PREFIXOS_ACEITOS.indexOfFirst { prefixo -> it.name.startsWith(prefixo) } }
        .flatMap { it.inetAddresses.asSequence().toList() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
        ?.hostAddress

    private fun interfacesCandidatas(): List<NetworkInterface> = runCatching {
        NetworkInterface.getNetworkInterfaces()
            ?.toList()
            ?.filter { rede ->
                rede.isUp &&
                    !rede.isLoopback &&
                    PREFIXOS_ACEITOS.any { rede.name.startsWith(it) }
            }
            .orEmpty()
    }.getOrDefault(emptyList())

    /** Em ordem de preferência: Wi-Fi, hotspot, Ethernet/emulador. */
    private val PREFIXOS_ACEITOS = listOf("wlan", "ap", "swlan", "eth")
}
