package run.halo.app.repository.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * Base repository interface contains some common methods.
 * 仓库类的基类
 * @param <DOMAIN> doamin type
 * @param <ID>     id type
 * @author johnniang
 * @author ryanwang
 * @date 2019-03-15
 */
//防止这个接口在启动的时候被实例化,因为这只是个中间接口,用来扩展功能的
// 在初始的情况下实例将会失败,为了避免启动失败用这个注解
@NoRepositoryBean
public interface BaseRepository<DOMAIN, ID> extends JpaRepository<DOMAIN, ID> {

    /**
     * Finds all domain by id list.
     *
     * @param ids  id list of domain must not be null
     * @param sort the specified sort must not be null
     * @return a list of domains
     */
    @NonNull
    List<DOMAIN> findAllByIdIn(@NonNull Collection<ID> ids, @NonNull Sort sort);

    /**
     * Finds all domain by domain id list.
     *
     * @param ids      must not be null
     * @param pageable must not be null
     * @return a list of domains
     */
    @NonNull
    Page<DOMAIN> findAllByIdIn(@NonNull Collection<ID> ids, @NonNull Pageable pageable);

    /**
     * Deletes by id list.
     *
     * @param ids id list of domain must not be null
     * @return number of rows affected
     */
    long deleteByIdIn(@NonNull Collection<ID> ids);

}
