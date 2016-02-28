import os

from flask import (Flask, request, send_file)
from flask_restful import Resource, Api
from flask_mongoengine import MongoEngine
from werkzeug.utils import secure_filename

from users import (User, UserImage)


MONGODB_DB = 'hacklondondb'
MONGODB_HOST = '104.46.48.140'
MONGODB_PORT = 27017
ALLOWED_EXTENSIONS = set(['jpg', 'png', 'jpeg'])
TMP_FOLDER = 'tmp'

# Create the application
app = Flask(__name__)
app.config.from_object(__name__)
api = Api(app)

# Initialize the MongoDB driver
db = MongoEngine(app)


def allowed_file(filename):
    """
    Check the file extension of the the uploaded file.

    Parameters
    ----------
    filename : string
        Filename.

    Returns
    -------
    is_allow : boolean
        "True" if the input filename is allowed, and "False" otherwise

    """
    return '.' in filename and \
        filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']


class GetUserResource(Resource):

    def get(self, fb_id):
        # Query for a user
        users = User.objects(facebook_id=fb_id)

        if len(users) > 1:
            raise Exception('Multiple facebook id detected.')
        elif len(users) == 1:
            user = users[0]
            return {'status': 'success', 'facebook_id': user.facebook_id}

        return {'status': 'fail', 'message': 'Invalid facebook id.'}


class AddUserInfoResource(Resource):

    def post(self):
        # Get user information from POST request
        # fb_id = request.form.get('facebook_id')
        # name = request.form.get('name')
        fb_id = request.json.get('facebook_id')
        name = request.json.get('name')

        if not fb_id or not name:
            return {'status': 'fail', 'message': 'Please specify facebook id and name.'}

        # Create a User object
        user = User()
        user.facebook_id = fb_id
        user.name = name
        user.save()

        # Return user information
        return {'status': 'success'}


class UploadUserImgResource(Resource):

    def post(self, fb_id):
        # Query for the patient
        users = User.objects(facebook_id=fb_id)

        if len(users) > 1:
            raise {'status': 'fail', 'message': 'Multiple user id detected.'}
        elif len(users) == 0:
            return {'status': 'fail', 'message': 'Invalid facebook id.'}

        user = users[0]

        # Create an instance for session
        user_img = UserImage()
        user_img.facebook_id = user.facebook_id

        # Get upload file
        file = request.files.get('file')
        if file:
            # Get the secure filename
            filename = secure_filename(file.filename)

            # Check for allowed extension
            if allowed_file(file.filename):
                # Save file to disk
                upload_file_path = os.path.join(app.config['TMP_FOLDER'], filename)
                file.save(upload_file_path)

                # If there is upload file
                if os.path.isfile(upload_file_path):
                    # Save the upload to the database using GridFS
                    with open(upload_file_path, 'rb') as data_file:
                        user_img.data_file.put(data_file, content_type=file.content_type)

                        # Create a user image
                        user_img.save()

                        # Remove uploaded file from the upload directory, as it has already been stored in database
                        os.remove(upload_file_path)

                        return {'status': 'success', 'user_img_id': str(user_img.user_img_id)}
                else:
                    return {'status': 'fail', 'message': 'Fail to save the sent file.'}
            else:
                return {'status': 'fail', 'message': 'Invalid file format.'}
        else:
            return {'status': 'fail', 'message': 'File not found. Please send a file using \'file\' as a name.'}




class ViewUserImgResource(Resource):

    def get(self, fb_id):
        user_img = UserImage.objects(facebook_id=fb_id)

        imgs = []
        for img in user_img:
            imgs.append(str(img.user_img_id))

        return {'status': 'success', 'imgs': imgs}


class DownloadUserImgResource(Resource):

    def get(self, fb_id, user_img_id):

        user_img = UserImage.objects(facebook_id=fb_id, user_img_id=user_img_id)

        if len(user_img) > 1:
            raise Exception('Multiple image id detected.')

        if len(user_img) > 1:
            raise {'status': 'fail', 'message': 'Multiple image id detected.'}
        elif len(user_img) == 0:
            return {'status': 'fail', 'message': 'Invalid facebook id and/or image id.'}

        img = user_img[0]

        # Read the data file
        img_file = img.data_file.read()

        # Save into a temporarily csv file onto the server, then send to the client
        filename = img.facebook_id + '_' + str(img.user_img_id) + '.jpg'
        filepath = os.path.join(app.config['TMP_FOLDER'], filename)
        with open(filepath, 'wb') as file:
            file.write(img_file)

        return send_file(filepath)


class GetUserFriendResource(Resource):

    def get(self, fb_id):

        users = User.objects(facebook_id=fb_id)

        if len(users) > 1:
            raise {'status': 'fail', 'message': 'Multiple facebook id detected.'}
        elif len(users) == 0:
            return {'status': 'fail', 'message': 'Invalid facebook id.'}

        user = users[0]

        list_friend = []
        for f in user.friends:
            list_friend.append(f.facebook_id)

        return {'status': 'success', 'friends': list_friend}

# User
api.add_resource(GetUserResource, '/user/<string:fb_id>')
api.add_resource(AddUserInfoResource, '/user/add')

# User images
api.add_resource(UploadUserImgResource, '/user/img/upload/<string:fb_id>')
api.add_resource(ViewUserImgResource, '/user/img/<string:fb_id>')
api.add_resource(DownloadUserImgResource, '/user/img/<string:fb_id>/<string:user_img_id>')

# User friends
api.add_resource(GetUserFriendResource, '/user/friend/<string:fb_id>')


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
