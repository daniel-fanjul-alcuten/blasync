package com.spotify.blasync;

public class RequestsTemplate {

	public UserWrapper request1(RequestContextWrapper context,
			StringWrapper username) {

		MessageWrapper message = context.getClient().send(
				new StringLiteralWrapper("hm://users/").concat(username));

		ObjectWrapper object = message.getObject();
		UserInfoCastWrapper userInfo = new UserInfoCastWrapper(object);
		StringWrapper userName = userInfo.getUsername();
		StringWrapper userData = userInfo.getMetadata();

		UserPojoWrapper user = new UserPojoWrapper();
		user = user.setUsername(userName);
		user = user.setMetadata(userData);

		EqualsWrapper ok = new EqualsWrapper(message.getCode(),
				new IntegerLiteralWrapper(200));
		return new UserIfWrapper(ok, user, new UserLiteralWrapper(null));
	}
}
