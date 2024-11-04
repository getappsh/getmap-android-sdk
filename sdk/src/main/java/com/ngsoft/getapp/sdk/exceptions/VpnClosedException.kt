package com.ngsoft.getapp.sdk.exceptions

import java.io.IOException


class VpnClosedException: IOException("The VPN connection is closed. Please reconnect to continue.")