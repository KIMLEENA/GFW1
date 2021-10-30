# swaggerModel.py

from flask_restx import Namespace, fields

SwaggerModel = Namespace('SwaggerModel')

# base success or failed model
BaseSuccessModel = SwaggerModel.model('Base Success Model', {
    "status" : fields.String(description="Success or Failed", example="Success")
})

BaseFailedModel = SwaggerModel.model('Base Failed Model', {
    "status" : fields.String(description="Success or Failed", example="Failed")
})

BaseProfileModel = SwaggerModel.model('Base Profile Model', {
    "name" : fields.String(description="유저의 이름", example="testName"),
})

# errorhandler에서 정의된 에러 return model
NoAuthModel = SwaggerModel.inherit('No Auth Model', BaseFailedModel, {
    "message" : fields.String(description="오류 메시지", example="Missing Authorization Header")
})

RevokedTokenModel = SwaggerModel.inherit('Revoked Token Model', BaseFailedModel, {
    "message" : fields.String(description="오류 메시지", example="Token has been revoked")
})

ExpiredTokenModel = SwaggerModel.inherit('Expired Token Model', BaseFailedModel, {
    "message" : fields.String(description="오류 메시지", example="Token has expired")
})

MethodNotAllowedModel = SwaggerModel.inherit('Method Not Allowed Model', BaseFailedModel, {
    "message" : fields.String(description="오류 메시지", example="The method is not allowed for the requested URL.")
})

InternalServerErrorModel = SwaggerModel.inherit('Internal Server Error Model', BaseFailedModel, {
    "message" : fields.String(description="오류 메시지", example="Internal Server Error")
})

InvalidRequestModel = SwaggerModel.inherit('Invalid Request Error Model', BaseFailedModel, {
    "message" : fields.String(description="오류 메시지", example="invalid JSON parameters")
})
