package net.laoyeye.yyblog.service.impl;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.qq.connect.QQConnectException;
import com.qq.connect.api.OpenID;
import com.qq.connect.api.qzone.UserInfo;
import com.qq.connect.javabeans.AccessToken;
import com.qq.connect.javabeans.qzone.UserInfoBean;
import com.qq.connect.oauth.Oauth;

import net.laoyeye.yyblog.common.SessionParam;
import net.laoyeye.yyblog.common.YYBlogResult;
import net.laoyeye.yyblog.common.utils.AESUtils;
import net.laoyeye.yyblog.common.utils.CookieUtils;
import net.laoyeye.yyblog.common.utils.IDUtils;
import net.laoyeye.yyblog.mapper.UserMapper;
import net.laoyeye.yyblog.model.User;
import net.laoyeye.yyblog.service.QqLoginService;

@Service
public class QqLoginServiceImpl implements QqLoginService {
	@Autowired
	private UserMapper userMapper;

	@Override
	public YYBlogResult login(HttpServletRequest request, HttpServletResponse response) {
		try {
			// 第一步获取授权码
			// 第二步获取accesstoken
			AccessToken accessTokenObj = new Oauth().getAccessTokenByRequest(request);
			String accessToken = accessTokenObj.getAccessToken();
			OpenID openidObj = new OpenID(accessToken);
			// 数据查找openid是否关联,如果没有关联先跳转到关联账号页面,如果直接登录.
			String userOpenId = openidObj.getUserOpenID();
			User user = userMapper.getUserByOpenId(userOpenId);
			if (StringUtils.isEmpty(user)) {
				//获取用户信息
				UserInfo qzoneUserInfo = new UserInfo(accessToken, userOpenId);
				UserInfoBean userInfoBean = qzoneUserInfo.getUserInfo();
				user = new User();
				user.setId(IDUtils.genId());
				user.setNickname(userInfoBean.getNickname());
				user.setAvatar(userInfoBean.getAvatar().getAvatarURL50());
				user.setOpenId(userOpenId);
				user.setCreateTime(new Date());
				user.setUpdateTime(new Date());
				//回头改，不能写死
				user.setRoleId((long)2);
				user.setEnable(true);
				//保存用户信息
				userMapper.save(user);
			}
			//生成token
			String token = AESUtils.encrypt(String.valueOf(user.getId())+","+System.currentTimeMillis(), 
					SessionParam.COOKIE_SECRET_KEY);
			//添加cookie的逻辑，关闭浏览器失效
			CookieUtils.setCookie(request, response, SessionParam.COOKIE_NAME, token);
			//设置session
			request.getSession().setAttribute(token, user);
			return YYBlogResult.ok();
		} catch (Exception e) {
			//LOG
			e.printStackTrace();
			return YYBlogResult.build(500, e.getMessage());
		}

	}

}
