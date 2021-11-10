# Friends.py

from flask import request
from flask_restx import Resource, Namespace, fields
from flask_jwt_extended.utils import get_jwt_identity, get_jwt
from flask_jwt_extended import jwt_required
from flask_request_validator import *
import database, swaggerModel

Friends = Namespace(name = "Friends", description="친구관련 작업을 처리하는 API")
parser = Friends.parser()
parser.add_argument('Authorization', location='headers', type=str, help='jwt 토큰이 사용된다.')
FriendsPostRequest = Friends.model('Friend post model', {
    "friendEmail" : fields.String(description="친구의 이메일", required=True, example="testemail2@testdomain.com")
})
FriendsPutRequest = Friends.model('Friend put model', {
    "friendEmail" : fields.String(description="친구의 이메일", required=True, example="testemail2@testdomain.com"),
    "accept" : fields.String(description="요청 수락 여부", required=True, example="yes/no"),
})
FriendsDeleteRequest = Friends.model('Friend delete model ', {
    "friendEmail" : fields.String(description="친구의 이메일", required=True, example="testemail2@testdomain.com")
})
FriendsGetRequest = Friends.model('Friend get model ', {
    "friendEmail" : fields.String(description="친구의 이메일", required=True, example="testemail2@testdomain.com")
})
FriendsGet2Request = Friends.model('Friend get2model ', {
    "friendEmail" : fields.String(description="친구의 이메일", required=True, example="testemail2@testdomain.com")
})
FriendsPutFailedRequest = Friends.inherit('Friend put failed model ', swaggerModel.BaseFailedModel, {
    "message" : fields.String(description="에러 메시지", example="already requested or exist")
})
FriendsPutFailed2Request = Friends.inherit('Friend put failed model 2 ', swaggerModel.BaseFailedModel, {
    "message" : fields.String(description="에러 메시지", example="aleardy friend")
})
FriendsPutFailed3Request = Friends.inherit('Friend put failed model 3', swaggerModel.BaseFailedModel, {
    "message" : fields.String(description="에러 메시지", example="Worng email")
})
FriendsSuccessModel = Friends.inherit('Friends Success model', swaggerModel.BaseSuccessModel,{
    "FriendsList" : fields.String(description="친구목록", example="[['test9@gmail.com', '페이커', 'http://180.80.221.11:5000/service/images/default_profile.jpg'], ['test3@gmail.com', '류현진', 'http://180.80.221.11:5000/service/images/default_profile.jpg']]"),
})
FriendsSuccessModel2 = Friends.inherit('Friends Success model2', swaggerModel.BaseSuccessModel,{
    "FriendsList" : fields.String(description="친구목록 2", example="{email : ['test9@gmail.com', 'test3@gmail.com'], name : ['페이커', '류현진'], profilePhoto : ['http://180.80.221.11:5000/service/images/default_profile.jpg', 'http://180.80.221.11:5000/service/images/default_profile.jpg']}"),
})
FriendsGetFailedRequest = Friends.inherit('Friend get failed model', swaggerModel.BaseFailedModel, {
    "message" : fields.String(description="에러 메시지", example="No friend")
})
FriendsGetFailedRequest2 = Friends.inherit('Friend get failed model2', swaggerModel.BaseFailedModel, {
    "message" : fields.String(description="에러 메시지", example="No request")
})


@Friends.route("/friends")
@Friends.response(500, 'Failed(서버 관련 이슈)', swaggerModel.InternalServerErrorModel)
class AppFriend(Resource):
    # 친구요청 보내기
    @validate_params(
        Param('friendEmail', JSON, str, rules=[Pattern(r'^[\w+-_.]+@[\w-]+\.[a-zA-Z-.]+$')]),   # 이메일 형식 체크
    )
    @jwt_required()
    @Friends.expect(parser, FriendsPostRequest)
    @Friends.response(200, 'Success', swaggerModel.BaseSuccessModel)
    @Friends.response(400, 'Failed (전에 이미 친구추가 요청을 보냈다.)', FriendsPutFailed3Request)   
    def post(self, *args):
        """ 사용자로부터 친구 이메일을 인자로 받아서 친구 요청을 보낸다."""
        requester = get_jwt_identity()
        data = request.json
        acceptor = data['friendEmail']
        print(data)

        db = database.DBClass()
        query = f'''
        SELECT id as id FROM request_friend where requester={requester} and acceptor={acceptor}
        UNION ALL 
        SELECT id as id FROM friends where user_email={requester} and user_friend_email{acceptor};
        '''
        if db.executeOne(query):
            return {"status" : "Failed", "message" : "already requested or exist"}, 400
        
        query = f'''
        insert into request_friend(requester, acceptor) values ({requester}, {acceptor});
        '''
        db.execute_and_commit(query)
        return {"status" : "success", "message" : "Your request message has been delivered"}, 200
    
    # 친구요청 수락, 거절
    @validate_params(
        Param('acceptor', JSON, str, rules=[Pattern(r'^[\w+-_.]+@[\w-]+\.[a-zA-Z-.]+$')]),   # 이메일 형식 체크
    )
    @jwt_required()
    @Friends.expect(parser, FriendsPutRequest)
    @Friends.doc(params= {"payload":"accept : 수락 여부(대소문자 구별 없음)"})
    @Friends.response(200, 'Success', swaggerModel.BaseSuccessModel)
    @Friends.response(400, 'Failed ( 친구 입력이 잘못되었다.)', FriendsPutFailedRequest)  
    @Friends.response(401, 'Failed (이미 친구이다.)', FriendsPutFailed2Request)  
    def put(self, *args, **kwargs):
        """친구요청을 수락하거나 거절한다."""
        requester = get_jwt_identity()
        data = request.json
        acceptor = data['acceptor']
        
        db = database.DBClass()
        
        # 입력이 잘못 된 경우
        query = f'''
        SELECT * FROM request_friend where requester={requester} and acceptor={acceptor}
        '''
        if not db.executeOne(query):
            return {"status" : "Failed", "message" : "wrong email"}, 400

        # 친구요청을 거절하는 경우
        if data["accept"].lower() in ["no", "false"]:
            query = f'''
            delete FROM request_friend where requester={requester} and acceptor={acceptor}
            '''
            db.execute_and_commit(query, data)
            return {"status" : "success", "message" : "Friend request has been denied"}, 200

        # 친구 요청을 수락하는 경우
        query1 = f'''
            delete FROM request_friend where requester={requester} and acceptor={acceptor}
            '''
        db.execute_and_commit(query1, data)
        
        query2 = f'''
        SELECT * FROM friends 
        where user_email={requester} and user_friend_email={acceptor}
        or user_email={acceptor} and user_friend_email={requester}
        ;
        '''
        ## 이미 친구인 경우
        if db.executeAll(query2, data):   
            return {"status" : "failed", "message" : "aleardy friend"}, 400

        ## 친구 요청 수락
        query3 ='''
        insert into friends(`user_email`, `user_friend_email`) values (%s,%s);
        '''
        db.execute_and_commit(query3, (data['requester'], data['acceptor']))
        db.execute_and_commit(query3, (data['acceptor'], data['requester']))
        return {"status" : "success", "message" : "Friend request has been accepted"}, 201
    
    #친구 삭제
    @validate_params(
        Param('friendEmail', JSON, str, rules=[Pattern(r'^[\w+-_.]+@[\w-]+\.[a-zA-Z-.]+$')]),   # 이메일 형식 체크
    )
    @jwt_required()
    @Friends.expect(parser, FriendsDeleteRequest)
    @Friends.response(200, 'Success', swaggerModel.BaseSuccessModel)
    @Friends.response(400, 'Failed ( 입력이 잘못되었다.)', FriendsPutFailed3Request)  
    def delete(self, *args, **kwargs):
        """나의 친구목록에서 친구를 삭제한다."""
        user = get_jwt_identity()
        data = request.json
        user_friend = data["friendEmail"]
        db = database.DBClass()
        # 입력이 잘못 된 경우
        query = f'''
        SELECT * FROM friends 
        where user_email={user} and user_friend_email={user_friend}
        or user_email={user_friend} and user_friend_email={user};
        '''
        if not db.executeOne(query):
            return {"status" : "Failed", "message" : "wrong email"}, 400
        
        # 친구 삭제
        query1 = '''
        SET SQL_SAFE_UPDATES=0;
        '''
        query2 = f'''
        delete from friends where user_email={user} and user_friend_email={user_friend}
        or user_email={user_friend} and user_friend_email={user};
        '''
        db.execute_and_commit(query1)
        db.execute_and_commit(query2)
        return {"status" : "success", "message" : "Your friend has been deleted"}, 200



@Friends.route("/friendsList")
class AppFriendList(Resource):
    @jwt_required()
    @Friends.expect(parser)
    @Friends.response(200, 'Success(친구 목록을 반환한다.)', FriendsSuccessModel)
    @Friends.response(400, 'Failed ( 친구가 없다. )', FriendsGetFailedRequest) 
    def get(self, *args, **kwargs):
        """사용자의 친구목록을 보여준다"""
        userEmail = get_jwt_identity()

        db = database.DBClass()
        query = f'''
            select email, name, profilePhoto from users, (select user_friend_email from friends where user_email = '{userEmail}') as gg where gg.user_friend_email = email;
        '''
        if (friendsList_dict := db.executeAll(query)):
            return {"status" : "success", "FriendsList" : f"{[list(i.values()) for i in friendsList_dict]}"},200
        else:
            return  {"status" : "Failed", "message" : "no friends"}, 400

@Friends.route("/friendsList_2")
class AppFriendList(Resource):
    @jwt_required()
    @Friends.expect(parser)
    @Friends.response(200, 'Success(친구 목록을 반환한다.)', FriendsSuccessModel2)
    @Friends.response(400, 'Failed ( 친구가 없다. )', FriendsGetFailedRequest) 
    def get(self, *args, **kwargs):
        """사용자의 친구목록을 보여준다"""
        userEmail = get_jwt_identity()

        db = database.DBClass()
        query = f'''
            select email, name, profilePhoto from users, (select user_friend_email from friends where user_email = '{userEmail}') as gg where gg.user_friend_email = email;
        '''
        if not(result := db.executeAll(query)):
            return  {"status" : "Failed", "message" : "no friends"}, 400
        email = []
        name =[]
        profilePhoto = []

        for i in result:
            email.append(i["email"])
            name.append(i["name"])
            profilePhoto.append(i["profilePhoto"])


        return {"status" : "success", "FriendsList" : 
        f"{{email : {email}, name : {name}, profilePhoto : {profilePhoto}}}"},200

@Friends.route("/friendsRequestList")
class AppFriendRequestList(Resource):
    @jwt_required()
    @Friends.expect(parser)
    @Friends.response(200, 'Success(친구 요청목록을 반환한다.)', FriendsSuccessModel)
    @Friends.response(400, 'Failed ( 친구 요청이 없다. )', FriendsGetFailedRequest2) 
    def get(self, *args, **kwargs):
        """사용자에게 친구요청을 보낸 유저목록을 보여준다"""
        userEmail = get_jwt_identity()

        db = database.DBClass()
        query = f'''
            select email, name, profilePhoto from users, (select requester from request_friend where acceptor = '{userEmail}') as gg where gg.requester = email;
        '''

        if (RequestList_dict := db.executeAll(query)):
            return {"status" : "success", "FriendsList" : f"{[list(i.values()) for i in RequestList_dict]}"},200
        else:
            return  {"status" : "Failed", "message" : "No request"}, 400
    
