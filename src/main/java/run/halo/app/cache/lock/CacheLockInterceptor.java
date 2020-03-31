package run.halo.app.cache.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import run.halo.app.cache.AbstractStringCacheStore;
import run.halo.app.exception.FrequentAccessException;
import run.halo.app.exception.ServiceException;
import run.halo.app.utils.ServletUtils;

import java.lang.annotation.Annotation;

/**
 * Interceptor for cache lock annotation.
 *
 * @author johnniang
 * @date 3/28/19
 */
@Slf4j
@Aspect
@Configuration
public class CacheLockInterceptor {

    private final static String CACHE_LOCK_PREFOX = "cache_lock_";

    private final static String CACHE_LOCK_VALUE = "locked";

    private final AbstractStringCacheStore cacheStore;

    public CacheLockInterceptor(AbstractStringCacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Around("@annotation(run.halo.app.cache.lock.CacheLock)")
    public Object interceptCacheLock(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method signature
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        log.debug("Starting locking: [{}]", methodSignature.toString());

        // Get cache lock
        CacheLock cacheLock = methodSignature.getMethod().getAnnotation(CacheLock.class);
        //2020年3月30日02:36:46,获取cacheLock有什么用呢?
        // Build cache lock key
        String cacheLockKey = buildCacheLockKey(cacheLock, joinPoint);

        log.debug("Built lock key: [{}]", cacheLockKey);


        try {
            // Get from cache
            //如果这个key失效了,创建个新的返回个false???
            Boolean cacheResult = cacheStore.putIfAbsent(cacheLockKey, CACHE_LOCK_VALUE, cacheLock.expired(), cacheLock.timeUnit());

            if (cacheResult == null) {
                throw new ServiceException("Unknown reason of cache " + cacheLockKey).setErrorData(cacheLockKey);
            }

            if (!cacheResult) {
                throw new FrequentAccessException("访问过于频繁，请稍后再试！").setErrorData(cacheLockKey);
            }

            // Proceed the method
            return joinPoint.proceed();
        } finally {
            // Delete the cache
            if (cacheLock.autoDelete()) {
                cacheStore.delete(cacheLockKey);
                log.debug("Deleted the cache lock: [{}]", cacheLock);
            }
        }
    }

    /**
     * 私有方法,做缓存key用的
     *
     * @param cacheLock
     * @param joinPoint
     * @return
     */
    private String buildCacheLockKey(@NonNull CacheLock cacheLock, @NonNull ProceedingJoinPoint joinPoint) {
        Assert.notNull(cacheLock, "Cache lock must not be null");
        Assert.notNull(joinPoint, "Proceeding join point must not be null");

        // Get the method
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        // Build the cache lock key
        StringBuilder cacheKeyBuilder = new StringBuilder(CACHE_LOCK_PREFOX);
        //1. 添加 "cache_lock_"
        String delimiter = cacheLock.delimiter();

        if (StringUtils.isNotBlank(cacheLock.prefix())) {
            //2.1 添加 "cache_lock_" + "从注解获得的prefix"
            cacheKeyBuilder.append(cacheLock.prefix());
        } else {
            //2.2 添加 "cache_lock_" + "添加方法名"
            cacheKeyBuilder.append(methodSignature.getMethod().toString());
        }

        // Handle cache lock key building
        //返回的是二位数组,参数的注解
        //一个参数可以有多个注解故二维数组才能表示清楚
        Annotation[][] parameterAnnotations = methodSignature.getMethod().getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            //第一个循环指是遍历所有参数
            log.debug("Parameter annotation[{}] = {}", i, parameterAnnotations[i]);

            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                //每个参数上的注解
                Annotation annotation = parameterAnnotations[i][j];
                log.debug("Parameter annotation[{}][{}]: {}", i, j, annotation);
                if (annotation instanceof CacheParam) {
                    // Get current argument
                    Object arg = joinPoint.getArgs()[i];
                    log.debug("Cache param args: [{}]", arg);

                    // Append to the cache key
                    //3. 添加 "cache_lock_" + "从注解获得的prefix" + "参数名"
                    cacheKeyBuilder.append(delimiter).append(arg.toString());
                }
            }
        }

        if (cacheLock.traceRequest()) {
            // Append http request info
            //3. 添加 "cache_lock_" + "从注解获得的prefix" + "参数名" + "ip"
            cacheKeyBuilder.append(delimiter).append(ServletUtils.getRequestIp());
        }

        return cacheKeyBuilder.toString();
    }
}
