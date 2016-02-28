using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;
using System.Threading.Tasks;
using Microsoft.ProjectOxford.Face;
using Microsoft.ProjectOxford.Face.Contract;

namespace who_is_that_guy.Controllers
{
    public class UploadphotoController : ApiController
    {
        public static readonly string GroupName = "who_is_that_guy";
        public static readonly string subscriptionKey = "ea453be3896546e6aeab7185a088c31a";
        /// <summary>
        /// Faces to identify
        /// </summary>
        private ObservableCollection<Face> _faces = new ObservableCollection<Face>();

        /// <summary>
        /// Person database
        /// </summary>
        private ObservableCollection<Person> _persons = new ObservableCollection<Person>();

        /// <summary>
        /// User picked image file path
        /// </summary>
        private string _selectedFile;

        /// <summary>
        /// Gets person database
        /// </summary>
        public ObservableCollection<Person> Persons
        {
            get
            {
                return _persons;
            }
        }

        /// <summary>
        /// Gets or sets user picked image file path
        /// </summary>
        public string SelectedFile
        {
            get
            {
                return _selectedFile;
            }
        }

        /// <summary>
        /// Gets faces to identify
        /// </summary>
        public ObservableCollection<Face> TargetFaces
        {
            get
            {
                return _faces;
            }
        }

        public int MaxImageSize
        {
            get
            {
                return 300;
            }
        }

        // PUT: api/Uploadphoto/5
        public static async void Put(string facebookID, string fileDir)
        {
            bool groupExists = false;
            var faceServiceClient = new FaceServiceClient(subscriptionKey);
            try
            {
                //MainWindow.Log("Request: Group {0} will be used for build person database. Checking whether group exists.", GroupName);

                await faceServiceClient.GetPersonGroupAsync(GroupName);
                groupExists = true;
                //MainWindow.Log("Response: Group {0} exists.", GroupName);
            }
            catch (FaceAPIException ex)
            {
                if (ex.ErrorCode != "PersonGroupNotFound")
                {
                    //MainWindow.Log("Response: {0}. {1}", ex.ErrorCode, ex.ErrorMessage);
                    return;
                }
                else
                {
                    //MainWindow.Log("Response: Group {0} does not exist before.", GroupName);
                }
            }
            if (!groupExists)
            {
                //MainWindow.Log("Request: Creating group \"{0}\"", GroupName);
                try
                {
                    await faceServiceClient.CreatePersonGroupAsync(GroupName, GroupName);
                    //MainWindow.Log("Response: Success. Group \"{0}\" created", GroupName);
                }
                catch (FaceAPIException ex)
                {
                    //MainWindow.Log("Response: {0}. {1}", ex.ErrorCode, ex.ErrorMessage);
                    return;
                }
            }
            var tag = facebookID;
            var faces = new ObservableCollection<Face>();
            string personName = tag;
            string personID = (await faceServiceClient.CreatePersonAsync(GroupName, personName)).PersonId.ToString();
            var fStream = File.OpenRead(fileDir);
            var persistFace = await faceServiceClient.AddPersonFaceAsync(GroupName, Guid.Parse(personID), fStream, fileDir);
            //return new Tuple<string, ClientContract.AddPersistedFaceResult>(value, persistFace);
            //await Task.WhenAll(tasks);
            try
            {
                // Start train person group
                //MainWindow.Log("Request: Training group \"{0}\"", GroupName);
                await faceServiceClient.TrainPersonGroupAsync(GroupName);

                // Wait until train completed
                while (true)
                {
                    await Task.Delay(1000);
                    var status = await faceServiceClient.GetPersonGroupTrainingStatusAsync(GroupName);
                    Console.WriteLine("Response: {0}. Group \"{1}\" training process is {2}", "Success", GroupName, status.Status);
                    if (status.Status != (Status)2)
                    {
                        break;
                    }
                }
            }
            catch (FaceAPIException ex)
            {
                //MainWindow.Log("Response: {0}. {1}", ex.ErrorCode, ex.ErrorMessage);
            }

        }

    }
}
