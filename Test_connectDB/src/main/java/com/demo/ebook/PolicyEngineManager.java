package com.demo.ebook;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ow2.authzforce.core.pdp.impl.BasePdpEngine;
import org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration;

public class PolicyEngineManager {

    private record EngineHolder(BasePdpEngine engine, PolicyFingerprint fingerprint) {}

    private final PolicyMetadataRepository policyMetadataRepository = new PolicyMetadataRepository();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Holds the PDP loaded in memory so normal requests can reuse the same engine.
    private volatile EngineHolder currentHolder;

    private final String pdpConf;
    private final String catalog;
    private final String extXsd;

    public PolicyEngineManager(String pdpConf, String catalog, String extXsd) {
        this.pdpConf = pdpConf;
        this.catalog = catalog;
        this.extXsd = extXsd;
    }

    public synchronized void init() throws Exception {
        PolicyFingerprint fingerprint = policyMetadataRepository.loadCurrentFingerprint();
        BasePdpEngine engine = buildEngine();
        currentHolder = new EngineHolder(engine, fingerprint);
        System.out.println("ENGINE INIT => " + fingerprint);
    }

    public void startAutoReload(long periodSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean reloaded = reloadIfChanged();
                if (!reloaded) {
                    System.out.println("ENGINE CHECK => no change");
                }
            } catch (Exception e) {
                System.err.println("AUTO RELOAD ERROR => " + e.getMessage());
                e.printStackTrace();
            }
        }, periodSeconds, periodSeconds, TimeUnit.SECONDS);
    }

    public synchronized boolean reloadIfChanged() throws Exception {
        PolicyFingerprint latest = policyMetadataRepository.loadCurrentFingerprint();

        if (currentHolder != null && Objects.equals(currentHolder.fingerprint(), latest)) {
            return false;
        }

        // Build the new engine first; keep the old engine alive if rebuilding fails.
        BasePdpEngine newEngine = buildEngine();
        EngineHolder oldHolder = currentHolder;
        currentHolder = new EngineHolder(newEngine, latest);

        closeQuietly(oldHolder);

        System.out.println("ENGINE AUTO-RELOADED => " + latest);
        return true;
    }

    public synchronized void manualReload() throws Exception {
        PolicyFingerprint latest = policyMetadataRepository.loadCurrentFingerprint();
        // Manual reload uses the same safe replacement flow as auto reload.
        BasePdpEngine newEngine = buildEngine();

        EngineHolder oldHolder = currentHolder;
        currentHolder = new EngineHolder(newEngine, latest);

        closeQuietly(oldHolder);

        System.out.println("ENGINE MANUAL-RELOADED => " + latest);
    }

    public BasePdpEngine getEngine() {
        if (currentHolder == null) {
            throw new IllegalStateException("PDP engine chưa được init");
        }
        return currentHolder.engine();
    }

    public PolicyFingerprint getCurrentFingerprint() {
        if (currentHolder == null) {
            throw new IllegalStateException("PDP engine chưa được init");
        }
        return currentHolder.fingerprint();
    }

    public void shutdown() {
        scheduler.shutdownNow();
        closeQuietly(currentHolder);
    }

    private BasePdpEngine buildEngine() throws Exception {
        System.setProperty("javax.xml.accessExternalSchema", "file");
        System.setProperty("javax.xml.accessExternalDTD", "file");

        PdpEngineConfiguration conf = PdpEngineConfiguration.getInstance(pdpConf, catalog, extXsd);
        return new BasePdpEngine(conf);
    }

    private void closeQuietly(EngineHolder holder) {
        if (holder == null) {
            return;
        }

        try {
            holder.engine().close();
        } catch (Exception e) {
            System.err.println("CLOSE OLD ENGINE WARNING => " + e.getMessage());
        }
    }
}
