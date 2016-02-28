import datetime

import mongoengine as db
from bson import ObjectId


class Friend(db.Document):
    facebook_id = db.StringField(name='facebook_id', max_length=255, required=True, unique=True, primary_key=True)
    name = db.StringField(max_length=255, required=True)


class User(db.Document):
    # user_id = db.ObjectIdField(default=ObjectId, required=True, unique=True, primary_key=True)
    facebook_id = db.StringField(name='facebook_id', max_length=255, required=True, unique=True, primary_key=True)
    name = db.StringField(max_length=255, required=True)
    person_id = db.StringField(max_length=255, required=True)
    friends = db.ListField(db.EmbeddedDocumentField('Friend'))
    created_at = db.DateTimeField(default=datetime.datetime.now, required=True)

    meta = {
        'allow_inheritance': True,
        'indexes': ['-created_at'],
        'ordering': ['-created_at']
    }


class UserImage(db.Document):
    user_img_id = db.ObjectIdField(default=ObjectId, required=True, unique=True, primary_key=True)
    # user_ref = db.ReferenceField('User', required=True, dbref=False)
    facebook_id = db.StringField(max_length=255, required=True)
    data_file = db.FileField(required=True)
