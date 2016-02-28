using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web;
using System.Web.Http;

namespace who_is_that_guy.Controllers
{
    public class DetectfaceController : ApiController
    {

        [Route("api/Detectface")]
        [HttpPost]
        public string[] ImageUpload()
        {
            var request = HttpContext.Current.Request;
            var filePath = "C:\\Temp\\" + request.Headers["filename"];
            using (var fs = new System.IO.FileStream(filePath, System.IO.FileMode.Create))
            {
                request.InputStream.CopyTo(fs);
            }



            //Detection code using FaceAPI here





            return new string[] { "First User", "Second User" };
        }

    }
}
