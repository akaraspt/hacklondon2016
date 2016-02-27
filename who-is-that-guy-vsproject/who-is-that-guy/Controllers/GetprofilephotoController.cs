using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace who_is_that_guy.Controllers
{
    public class GetprofilephotoController : ApiController
    {
        // GET: api/Getprofilephoto
        public IEnumerable<string> Get()
        {
            return new string[] { "value1", "value2" };
        }

        // GET: api/Getprofilephoto/5
        public string Get(int id)
        {
            return "value";
        }

        // POST: api/Getprofilephoto
        public void Post([FromBody]string value)
        {
        }

        // PUT: api/Getprofilephoto/5
        public void Put(int id, [FromBody]string value)
        {
        }

        // DELETE: api/Getprofilephoto/5
        public void Delete(int id)
        {
        }
    }
}
