# Welcome to Cloud Functions for Firebase for Python!
# To get started, simply uncomment the below code or create your own.
# Deploy with `firebase deploy`

from firebase_functions import https_fn
from firebase_admin import initialize_app

# initialize_app()
#
#
# @https_fn.on_request()
# def on_request_example(req: https_fn.Request) -> https_fn.Response:
#     return https_fn.Response("Hello world!")
import functions_framework
from google.cloud import aiplatform
from google.protobuf import json_format
from google.protobuf.struct_pb2 import Value
import json

@functions_framework.http
def predict(request):
    if request.method != 'POST':
        return 'Send a POST request'
    content = request.json

    project = 'YOUR_PROJECT_ID'
    location = 'YOUR_LOCATION'
    endpoint_id = 'YOUR_ENDPOINT_ID'

    aiplatform.init(project=project, location=location)
    endpoint = aiplatform.Endpoint(endpoint_id)

    instance = json_format.ParseDict(content, Value())
    prediction = endpoint.predict([instance])

    return json.dumps(prediction.predictions)