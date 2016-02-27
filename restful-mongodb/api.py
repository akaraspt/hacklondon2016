import os

from flask import (Flask, request)
from flask_restful import Resource, Api
from flask_mongoengine import MongoEngine
from werkzeug.utils import secure_filename

import requests

from users import (User, UserImage)


MONGODB_DB = 'hacklondondb'
MONGODB_HOST = '127.0.0.1'
MONGODB_PORT = 27017
ALLOWED_EXTENSIONS = set(['jpg', 'png', 'jpeg'])
UPLOAD_FOLDER = 'tmp'

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

    def get(self, user_id):
        # Query for a user
        users = User.objects(user_id=user_id)

        list_users = ''
        for u_idx, u in enumerate(users):
            if u_idx == len(users) - 1:
                list_users += u.facebook_id
            else:
                list_users += u.facebook_id + ', '

        # Return user information
        return 'Found {}: {}'.format(len(users), list_users)


class AddUserInfoResource(Resource):

    def post(self):
        # Get user information from POST request
        fname = request.form.get('first_name')
        lname = request.form.get('last_name')
        fb_id = request.form.get('facebook_id')

        # Create a User object
        user = User()
        user.first_name = fname
        user.last_name = lname
        user.facebook_id = fb_id
        user.save()

        # Return user information
        return {str(user.user_id): fname + ' ' + lname + ' (' + fb_id + ')'}


class UploadUserImgResource(Resource):

    def post(self, user_id):
        # Query for the patient
        users = User.objects(user_id=user_id)

        if len(users) > 1:
            raise Exception('Multiple user id detected.')

        # Create an instance for session
        user_img = UserImage()
        user_img.user_ref = users[0]

        # Get upload file
        file = request.files.get('file')
        if file:
            # Get the secure filename
            filename = secure_filename(file.filename)

            # Check for allowed extension
            if allowed_file(file.filename):
                # Save file to disk
                upload_file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
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

                        return {'Status': 'Success'}

        return {'Status': 'Fail'}


class ViewUserImgResource(Resource):

    def get(self, user_id):
        user_img = UserImage.objects(user_ref=user_id)

        for img in user_img:
            # Read the data file
            img_file = img.data_file.read()

            # Save into a temporarily csv file onto the server, then send to the client
            filename = str(img.user_img_id) + '.jpg'
            with open(os.path.join(app.config['UPLOAD_FOLDER'], filename), 'wb') as file:
                file.write(img_file)

        return {'Status': 'Success'}


api.add_resource(GetUserResource, '/user/<string:user_id>')
api.add_resource(AddUserInfoResource, '/user/add')
api.add_resource(UploadUserImgResource, '/user/img/upload/<string:user_id>')
api.add_resource(ViewUserImgResource, '/user/img/<string:user_id>')


if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)