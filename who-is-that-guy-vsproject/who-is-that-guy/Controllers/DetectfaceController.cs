using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace who_is_that_guy.Controllers
{
    public class DetectfaceController : ApiController
    {

        // POST: api/Detectface
        public void Post([FromBody]string value)
        {
        }

        // PUT: api/Detectface/5
        public void Put(int id, [FromBody]string value)
        {
        }

        // DELETE: api/Detectface/5
        public void Delete(int id)
        {
        }
    }
}
