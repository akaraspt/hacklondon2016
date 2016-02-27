using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace who_is_that_guy.Controllers
{
    public class IsregisteredController : ApiController
    {

        // GET: api/isregistered/12345
        public string[] Get(string id)
        {
            return new string[]
            {
             "false",
             id
            };
        }
    }
}
