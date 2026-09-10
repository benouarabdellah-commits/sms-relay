package com.passerelle.sms.net

import java.net.Inet4Address
import java.net.NetworkInterface

object WifiInfo {
    fun ipv4Addresses(): List<String> {
        val result = mutableListOf<String>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
        for (iface in interfaces) {
            if (!iface.isUp || iface.isLoopback) continue
            for (address in iface.inetAddresses) {
                if (address is Inet4Address && address.isSiteLocalAddress) {
                    result += address.hostAddress ?: continue
                }
            }
        }
        return result.distinct()
    }

    fun primaryIpv4(): String? = ipv4Addresses().firstOrNull()

    fun isPrivateIpv4(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        var ip = host.trim().removePrefix("/").substringBefore("%")
        if (ip.startsWith("::ffff:")) ip = ip.removePrefix("::ffff:")
        return when {
            ip.startsWith("10.") -> true
            ip.startsWith("192.168.") -> true
            ip.startsWith("172.") -> {
                val second = ip.split(".").getOrNull(1)?.toIntOrNull() ?: return false
                second in 16..31
            }
            ip == "127.0.0.1" || ip == "::1" || ip == "localhost" -> true
            else -> false
        }
    }
}
