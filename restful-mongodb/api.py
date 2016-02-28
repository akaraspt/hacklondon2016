import os

import requests

from flask import (Flask, request, send_file)
from flask.ext.script import Manager, Server
from flask_restful import Resource, Api
from flask_mongoengine import MongoEngine
from werkzeug.utils import secure_filename

from users import (User, UserImage, Friend)


MONGODB_DB = 'hacklondondb'
MONGODB_HOST = '104.46.48.140'
MONGODB_PORT = 27017
ALLOWED_EXTENSIONS = set(['jpg', 'png', 'jpeg'])
TMP_FOLDER = '/Users/akara/Workspace/hacklondon2016/restful-mongodb/tmp'

# Create the application
app = Flask(__name__)
app.config.from_object(__name__)
api = Api(app)

# Initialize the MongoDB driver
db = MongoEngine(app)

manager = Manager(app)

# Turn on debugger by default and reloader
manager.add_command("runserver", Server(
    use_debugger = True,
    use_reloader = True,
    host = '0.0.0.0')
)

# Face API
persongroups_id = 'who_is_that_guy'
sub_key = 'ea453be3896546e6aeab7185a088c31a'


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
            list_friends = []
            for f in user.friends:
                list_friends.append({
                    'facebook_id': f.facebook_id,
                    'name': f.name
                })
            return {'status': 'success', 'friends': list_friends}

        return {'status': 'fail', 'message': 'Invalid facebook id.'}


class AddUserInfoResource(Resource):

    def post(self):
        # Get user information from POST request
        # fb_id = request.form.get('facebook_id')
        # name = request.form.get('name')
        fb_id = request.json.get('facebook_id')
        name = request.json.get('name')
        friends = request.json.get('friends')

        if not fb_id or not name:
            return {'status': 'fail', 'message': 'Please specify facebook id and name.'}

        # Create a User object
        user = User()
        user.facebook_id = fb_id
        user.name = name

        for f in friends:
            friend = Friend()
            friend.facebook_id = f.get('facebook_id')
            friend.name = f.get('name')
            user.friends.append(friend)

        # Connect to Face API
        response = requests.post(
            'https://api.projectoxford.ai/face/v1.0/persongroups/{}/persons'.format(persongroups_id),
            json={
                'name': fb_id,
                'userData': name
            },
            headers={
                'Ocp-Apim-Subscription-Key': sub_key
            }
        )

        response_json = response.json()
        if response_json.get('error'):
            return {'status': 'fail', 'message': response_json.get('error')}

        user.person_id = response_json.get('personId')

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

                    # ====================================================================================

                    # Upload image file
                    with open(upload_file_path, 'rb') as data_file:
                        response = requests.post(
                            'https://api.projectoxford.ai/face/v1.0/persongroups/{}/persons/{}/persistedFaces'.format(
                                persongroups_id,
                                user.person_id
                            ),
                            headers={
                                'Content-Type': 'application/octet-stream',
                                'Ocp-Apim-Subscription-Key': sub_key
                            },
                            data=data_file.read()
                        )

                        response_json = response.json()
                        if response_json.get('error'):
                            return {'status': 'fail', 'message': response_json.get('error')}

                    # Remove uploaded file from the upload directory, as it has already been stored in database
                    os.remove(upload_file_path)

                    # ====================================================================================

                    # Re-train the model, there is no need to wait for the system to finish, so ignore the response
                    response = requests.post(
                        'https://api.projectoxford.ai/face/v1.0/persongroups/{}/train'.format(
                            persongroups_id
                        ),
                        headers={
                            'Ocp-Apim-Subscription-Key': sub_key
                        }
                    )

                    # ====================================================================================

                    # Report training status
                    response = requests.get(
                        'https://api.projectoxford.ai/face/v1.0/persongroups/{}/training'.format(
                            persongroups_id
                        ),
                        headers={
                            'Ocp-Apim-Subscription-Key': sub_key
                        }
                    )
                    response_json = response.json()
                    if response_json.get('error'):
                        return {'status': 'fail', 'message': response_json.get('error')}

                    return {'status': 'success', 'user_img_id': str(user_img.user_img_id),
                            'train_status': response_json}
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


class DetectUserResource(Resource):

    def post(self):

        # Check training status
        response = requests.get(
            'https://api.projectoxford.ai/face/v1.0/persongroups/{}/training'.format(
                persongroups_id
            ),
            headers={
                'Ocp-Apim-Subscription-Key': sub_key
            }
        )
        response_json = response.json()
        if response_json.get('error'):
            return {'status': 'fail', 'message': response_json.get('error')}
        if response_json.get('status') != 'succeeded':
            return {'status': 'fail', 'message': 'The system is busy. Please try again.'}

        # ====================================================================================

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
                    # Send upload file to detect faces
                    with open(upload_file_path, 'rb') as data_file:
                        response = requests.post(
                            'https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=true&returnFaceAttributes=age,gender',
                            headers={
                                'Content-Type': 'application/octet-stream',
                                'Ocp-Apim-Subscription-Key': sub_key
                            },
                            data=data_file.read()
                        )

                        response_json = response.json()
                        if not isinstance(response_json, list):
                            if response_json.get('error'):
                                return {'status': 'fail', 'message': response_json.get('error')}

                    # Remove uploaded file from the upload directory, as it has already been stored in database
                    os.remove(upload_file_path)

                    # Get return face_id, which are used to refer to the detected faces
                    list_face_id = []
                    dict_face_id = {}
                    for r in response_json:
                        list_face_id.append(r.get('faceId'))
                        dict_face_id[r.get('faceId')] = r.get('faceRectangle')

                    if len(list_face_id) == 0:
                        return {'status': 'fail', 'message': 'No face detected.'}

                    # ====================================================================================

                    # Identify which user in the database has similar faces to the detected ones
                    response = requests.post(
                        'https://api.projectoxford.ai/face/v1.0/identify',
                        headers={
                            'Ocp-Apim-Subscription-Key': sub_key
                        },
                        json={
                            'personGroupId': persongroups_id,
                            'faceIds': list_face_id,
                            'maxNumOfCandidatesReturned': 1
                        }
                    )

                    response_json = response.json()
                    if not isinstance(response_json, list):
                        if response_json.get('error'):
                            return {'status': 'fail', 'message': response_json.get('error')}

                    list_person_id = []
                    dict_person_id = {}
                    for r in response_json:
                        for c in r.get('candidates'):
                            list_person_id.append(c.get('personId'))
                            dict_person_id[c.get('personId')] = r.get('faceId')

                    if len(list_person_id) == 0:
                        return {'status': 'fail', 'message': 'No user detected.'}

                    # ====================================================================================

                    # Get facebook id
                    list_facebook_id = []
                    dict_facebook_id = {}
                    for pid in list_person_id:
                        users = User.objects(person_id=pid)

                        if len(users) > 1:
                            raise {'status': 'fail', 'message': 'Multiple person id detected.'}
                        elif len(users) == 0:
                            return {'status': 'fail', 'message': 'Invalid person id ({}).'.format(pid)}

                        user = users[0]
                        list_facebook_id.append(user.facebook_id)
                        dict_facebook_id[user.facebook_id] = dict_face_id[dict_person_id[pid]]

                    return {'status': 'success',
                            'face_id': list_face_id,
                            'person_id': list_person_id,
                            'facebook_id': list_facebook_id,
                            'dict': dict_facebook_id}
                else:
                    return {'status': 'fail', 'message': 'Fail to save the sent file.'}
            else:
                return {'status': 'fail', 'message': 'Invalid file format.'}
        else:
            return {'status': 'fail', 'message': 'File not found. Please send a file using \'file\' as a name.'}


# User
api.add_resource(GetUserResource, '/user/<string:fb_id>')
api.add_resource(AddUserInfoResource, '/user/add')

# User images
api.add_resource(UploadUserImgResource, '/user/img/upload/<string:fb_id>')
api.add_resource(ViewUserImgResource, '/user/img/<string:fb_id>')
api.add_resource(DownloadUserImgResource, '/user/img/<string:fb_id>/<string:user_img_id>')

# Detect images
api.add_resource(DetectUserResource, '/user/detect')

# User friends
api.add_resource(GetUserFriendResource, '/user/friend/<string:fb_id>')


if __name__ == '__main__':
    # app.run(host='0.0.0.0', port=5000, debug=True)
    manager.run()
