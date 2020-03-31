package run.halo.app.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import run.halo.app.exception.ServiceException;
import run.halo.app.utils.JsonUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * String cache store.
 *缓存
 * @author johnniang
 */
@Slf4j
public abstract class AbstractStringCacheStore extends AbstractCacheStore<String, String> {

    public <T> void putAny(String key, T value) {
        try {
            put(key, JsonUtils.objectToJson(value));
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to convert " + value + " to json", e);
        }
    }

    /**
     * 2020年3月29日13:46:10,就目前已知的情况,这个方法用来缓存密码和令牌
     *  2020年3月31日01:33:05 缓存还缓存软件的配置信息
     * @param key      一个key
     * @param value    密码或者令牌
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @param <T>
     */
    public <T> void putAny(@NonNull String key, @NonNull T value, long timeout, @NonNull TimeUnit timeUnit) {
        try {
            put(key, JsonUtils.objectToJson(value), timeout, timeUnit);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to convert " + value + " to json", e);
        }
    }

    /**
     *
     * @param key 配置文件中存在缓存里的key
     * @param type
     * @param <T>
     * @return
     */
    public <T> Optional<T> getAny(String key, Class<T> type) {
        Assert.notNull(type, "Type must not be null");
        //根据key返回了缓存的包装类
        return get(key).map(value -> {
            try {//CacheWraper是个map?
                return JsonUtils.jsonToObject(value, type);
            } catch (IOException e) {
                log.error("Failed to convert json to type: " + type.getName(), e);
                return null;
            }
        });
    }
}
