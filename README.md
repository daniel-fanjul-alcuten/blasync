Description
===========

Proof of concept to generate java source files with asynchronous code for guava
(ListenableFuture's) given a graph of objects that represent the operations
that yield the results.

The graph of objects can be generated with java code, in a functional way,
making invocations that are similar to the generated invocations and that can
be compiled as usual.

The goal is to be able to focus on the business logic, without conditionals,
loops or asynchronous boilerplate.

There was time to implement only the conditionals and the asynchronous code.
The traversal of the graph is not complete either, some cases are not
supported: when the intersection of some sets of nodes in the graph is not
empty, i.e. The generated methods of the Wrapper classes are limited in several
ways too.


Details
=======

It generates code like this:


    package com.spotify.blasync;

    import java.util.List;
    import com.google.common.util.concurrent.AsyncFunction;
    import com.google.common.util.concurrent.Futures;
    import com.google.common.util.concurrent.ListenableFuture;

    public class Requests {


        public ListenableFuture<User> request1(final RequestContext p0, final String p1) {
            Client v0 = p0 .getClient();
            String v1 = "hm://users/".concat(p1);
            ListenableFuture<Message> v2 = v0 .send(v1);
            final ListenableFuture<Message> v3 = v2;
            @SuppressWarnings("unchecked")
            ListenableFuture<List<Object>> v4 = Futures.<Object>allAsList(v3);
            ListenableFuture<User> v14 = Futures.transform(v4, new AsyncFunction<Object,User>() {


                public ListenableFuture<User> apply(Object _)
                    throws Exception
                {
                    Message v5 = Futures.getUnchecked(v3);
                    Integer v6 = v5 .getCode();
                    Boolean v7 = v6 .equals(200);
                    User v8;
                    if (v7) {
                        User v9 = new User();
                        Object v10 = v5 .getObject();
                        UserInfo v11 = ((UserInfo) v10);
                        String v12 = v11 .getUsername();
                        v9 .setUsername(v12);
                        String v13 = v11 .getMetadata();
                        v9 .setMetadata(v13);
                        v8 = v9;
                    } else {
                        v8 = null;
                    }
                    return Futures.immediateFuture(v8);
                }

            }
            );
            return v14;
        }

    }


from code like this:


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


where the Wrapper classes are also generated.
