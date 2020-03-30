package run.halo.app.security.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Access token.
 *访问令牌主要有这几个属性
 *<br></br>
 * 1. token字符串accessToken
 * 2. 过期时间 expiredIn 在这个时间后过期
 * 3. refreshToken ??????
 * @author johnniang
 * @date 19-4-29
 */
@Data
public class AuthToken {

    /**
     * Access token.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Expired in. (seconds)
     */
    @JsonProperty("expired_in")
    private int expiredIn;

    /**
     * Refresh token.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;
}
