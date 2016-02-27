using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace who_is_that_guy.Controllers
{
    public class RegisterController : ApiController
    {
        public string[] Get()
        {
            return new string[]
            {
             "Hello",
             "world"
            };
        }
    }
}
