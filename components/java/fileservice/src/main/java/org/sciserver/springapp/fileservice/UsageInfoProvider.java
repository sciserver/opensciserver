package org.sciserver.springapp.fileservice;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.springapp.fileservice.dao.QuotaFromManager;
import org.sciserver.springapp.fileservice.dao.QuotaManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sciserver.springapp.loginterceptor.Log;

@Service
public class UsageInfoProvider {
    private static final Logger LOG = LogManager.getLogger(UsageInfoProvider.class);

    static final String ID_CACHE_NAME = "usageInfo";
    static final String ID_CACHE_KEY = "usageInfo";
    private final QuotaManagerService quotaManagerService;
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    public UsageInfoProvider(Optional<QuotaManagerService> quotaManagerService) {
        this.quotaManagerService = quotaManagerService.orElse(null);
    }

    public Collection<QuotaFromManager> getUsage() throws IOException {
        Log.get().setAttr("usageCacheHit", "0");
        Cache cache = cacheManager.getCache(ID_CACHE_NAME);
        ValueWrapper cacheval = cache.get(ID_CACHE_KEY);
        if (cacheval != null) {
            Log.get().setAttr("usageCacheHit", "1");
        }
        else {
            updateUsageCache();
            cacheval = cache.get(ID_CACHE_KEY);
        }
        return (Collection<QuotaFromManager>) cacheval.get();
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000 ,  initialDelay = 500)
    public void updateUsageCache() throws IOException {
        Cache cache = cacheManager.getCache(ID_CACHE_NAME);
        Collection<QuotaFromManager> quota;
        if (quotaManagerService == null) {
            quota = Collections.emptyList();
        } else {
            quota = quotaManagerService.getUsage().execute().body();
        }
        cache.put(ID_CACHE_KEY, quota);
    }
}
