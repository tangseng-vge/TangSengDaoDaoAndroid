package com.bage.im.message;

import android.os.Build;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Supplies xSocket with an Android system-trusted client TLS context.
 *
 * <p>xSocket 2.8.15 creates its {@link SSLEngine} without a peer host. That disables SNI and
 * hostname verification even when certificate-chain validation succeeds. This wrapper retains
 * the real tcp_addr host and injects it whenever xSocket asks for a no-argument engine.</p>
 */
final class BageClientSSLContext extends SSLContext {
    private BageClientSSLContext(SSLContext delegate, String peerHost, int peerPort) {
        super(new ClientContextSpi(delegate, peerHost, peerPort),
                delegate.getProvider(), delegate.getProtocol());
    }

    static SSLContext create(String peerHost, int peerPort) throws GeneralSecurityException {
        if (peerHost == null || peerHost.trim().isEmpty()) {
            throw new GeneralSecurityException("TLS peer host is empty");
        }
        if (peerPort <= 0 || peerPort > 65535) {
            throw new GeneralSecurityException("TLS peer port is invalid: " + peerPort);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // Endpoint identification on SSLEngine is not available before API 24. Fail closed
            // rather than silently accepting a certificate issued for a different host.
            throw new GeneralSecurityException("Verified IM TLS requires Android 7.0 or newer");
        }

        SSLContext delegate = SSLContext.getInstance("TLS");
        delegate.init(null, null, null);
        return new BageClientSSLContext(delegate, peerHost.trim(), peerPort);
    }

    private static final class ClientContextSpi extends SSLContextSpi {
        private final SSLContext delegate;
        private final String peerHost;
        private final int peerPort;

        private ClientContextSpi(SSLContext delegate, String peerHost, int peerPort) {
            this.delegate = delegate;
            this.peerHost = peerHost;
            this.peerPort = peerPort;
        }

        @Override
        protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers,
                                  SecureRandom secureRandom) throws java.security.KeyManagementException {
            delegate.init(keyManagers, trustManagers, secureRandom);
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return delegate.getSocketFactory();
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return delegate.getServerSocketFactory();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            return createVerifiedEngine(peerHost, peerPort);
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            String resolvedHost = host == null || host.trim().isEmpty() ? peerHost : host.trim();
            int resolvedPort = port > 0 ? port : peerPort;
            return createVerifiedEngine(resolvedHost, resolvedPort);
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return delegate.getServerSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return delegate.getClientSessionContext();
        }

        @Override
        protected SSLParameters engineGetDefaultSSLParameters() {
            return delegate.getDefaultSSLParameters();
        }

        @Override
        protected SSLParameters engineGetSupportedSSLParameters() {
            return delegate.getSupportedSSLParameters();
        }

        private SSLEngine createVerifiedEngine(String host, int port) {
            SSLEngine engine = delegate.createSSLEngine(host, port);
            engine.setUseClientMode(true);

            List<String> secureProtocols = new ArrayList<>(2);
            for (String protocol : engine.getSupportedProtocols()) {
                if ("TLSv1.3".equals(protocol) || "TLSv1.2".equals(protocol)) {
                    secureProtocols.add(protocol);
                }
            }
            if (secureProtocols.isEmpty()) {
                throw new IllegalStateException("TLS 1.2 or newer is unavailable");
            }
            engine.setEnabledProtocols(secureProtocols.toArray(new String[0]));

            SSLParameters parameters = engine.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            try {
                parameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
            } catch (IllegalArgumentException ignored) {
                // Literal IPv4/IPv6 addresses are verified through the certificate IP SAN and
                // are not valid SNI host_name values.
            }
            engine.setSSLParameters(parameters);
            return engine;
        }
    }
}
