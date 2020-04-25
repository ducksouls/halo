package run.halo.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import run.halo.app.config.properties.HaloProperties;
import run.halo.app.event.StaticStorageChangedEvent;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static run.halo.app.utils.HaloUtils.URL_SEPARATOR;
import static run.halo.app.utils.HaloUtils.ensureBoth;

/**
 * @author ryanwang
 * @date 2020-03-24
 */

// 自定义的,对controller里requestmapping 注解过方法进行处理的一个类

@Slf4j
public class HaloRequestMappingHandlerMapping extends RequestMappingHandlerMapping implements ApplicationListener<StaticStorageChangedEvent> {

    /**
     * 黑匹配???感觉应该是白名单啊???
     */
    private final Set<String> blackPatterns = new HashSet<>(16);

    /**
     * 路经匹配器
     */
    private final PathMatcher pathMatcher;

    private final HaloProperties haloProperties;

    public HaloRequestMappingHandlerMapping(HaloProperties haloProperties) {
        this.haloProperties = haloProperties;
        this.initBlackPatterns();
        pathMatcher = new AntPathMatcher();
    }

    /**
     * 根据请求的路径,也就是lookupPath去匹配
     */
    @Override
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
        log.debug("Looking path: [{}]", lookupPath);
        for (String blackPattern : blackPatterns) {
            if (this.pathMatcher.match(blackPattern, lookupPath)) {
                log.debug("Skipped path [{}] with pattern: [{}]", lookupPath, blackPattern);
                return null;
            }
        }
        return super.lookupHandlerMethod(lookupPath, request);
    }

    private void initBlackPatterns() {
        String uploadUrlPattern = ensureBoth(haloProperties.getUploadUrlPrefix(), URL_SEPARATOR) + "**";
        String adminPathPattern = ensureBoth(haloProperties.getAdminPath(), URL_SEPARATOR) + "?*/**";
        //看起来都是些静态资源
        blackPatterns.add("/themes/**");
        blackPatterns.add("/js/**");
        blackPatterns.add("/images/**");
        blackPatterns.add("/fonts/**");
        blackPatterns.add("/css/**");
        blackPatterns.add("/assets/**");
        blackPatterns.add("/color.less");
        blackPatterns.add("/swagger-ui.html");
        blackPatterns.add("/csrf");
        blackPatterns.add("/webjars/**");
        blackPatterns.add(uploadUrlPattern);
        blackPatterns.add(adminPathPattern);
    }

    @Override
    public void onApplicationEvent(StaticStorageChangedEvent event) {
        Path staticPath = event.getStaticPath();
        //括号里的内容支持包括流以及任何可关闭的资源，数据流会在 try 执行完毕后自动被关闭，而不用我们手动关闭了
        try (Stream<Path> rootPathStream = Files.list(staticPath)) {
            synchronized (this) {
                blackPatterns.clear();
                initBlackPatterns();
                rootPathStream.forEach(rootPath -> {
                            if (Files.isDirectory(rootPath)) {
                                String directoryPattern = "/" + rootPath.getFileName().toString() + "/**";
                                blackPatterns.add(directoryPattern);
                                log.debug("Exclude for folder path pattern: [{}]", directoryPattern);
                            } else {
                                String pathPattern = "/" + rootPath.getFileName().toString();
                                blackPatterns.add(pathPattern);
                                log.debug("Exclude for file path pattern: [{}]", pathPattern);
                            }
                        }
                );
            }
        } catch (IOException e) {
            log.error("Failed to refresh static directory mapping", e);
        }
    }
}
