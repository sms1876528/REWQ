using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace SUB3
{
    public class Program
    {
        // model -> agent set, loaded from MODELS.JSON
        static Dictionary<string, HashSet<string>> Models;
        // raw monitoring records (agentId, requestId, timestamp, dataType, dataValue)
        static List<JObject> Records = new List<JObject>();

        public static void Main(string[] args)
        {
            var raw = JsonConvert.DeserializeObject<Dictionary<string, List<string>>>(
                File.ReadAllText("MODELS.JSON"));
            Models = new Dictionary<string, HashSet<string>>();
            foreach (var kv in raw) Models[kv.Key] = new HashSet<string>(kv.Value);

            var listener = new HttpListener();
            listener.Prefixes.Add("http://127.0.0.1:8080/");
            listener.Start();

            while (true)
            {
                var ctx = listener.GetContext();
                var path = ctx.Request.Url.AbsolutePath;
                var body = new StreamReader(ctx.Request.InputStream).ReadToEnd();

                if (path == "/monitoring")
                {
                    Records.Add(JObject.Parse(body));
                    Reply(ctx, null);
                }
                else if (path == "/performance")
                {
                    var q = JObject.Parse(body);
                    Reply(ctx, Performance((string)q["modelName"], (string)q["timeWindow"]));
                }
            }
        }

        static string Performance(string modelName, string timeWindow)
        {
            var agents = Models[modelName];
            // key = agentId|requestId
            var pred = new Dictionary<string, int>();
            var pTs = new Dictionary<string, string>();
            var act = new Dictionary<string, int>();

            foreach (var r in Records)
            {
                var aid = (string)r["agentId"];
                if (!agents.Contains(aid)) continue;
                var key = aid + "|" + (string)r["requestId"];
                var type = (string)r["dataType"];
                if (type == "P") { pred[key] = (int)r["dataValue"]; pTs[key] = (string)r["timestamp"]; }
                else             { act[key]  = (int)r["dataValue"]; }
            }

            int correct = 0, total = 0;
            foreach (var kv in pred)
            {
                if (!pTs[kv.Key].StartsWith(timeWindow)) continue;
                total++;
                int a;
                if (act.TryGetValue(kv.Key, out a) && a == kv.Value) correct++;
            }
            return JsonConvert.SerializeObject(new { correct = correct, total = total });
        }

        static void Reply(HttpListenerContext ctx, string json)
        {
            ctx.Response.StatusCode = 200;
            if (json != null)
            {
                ctx.Response.ContentType = "application/json";
                var buf = Encoding.UTF8.GetBytes(json);
                ctx.Response.ContentLength64 = buf.Length;
                ctx.Response.OutputStream.Write(buf, 0, buf.Length);
            }
            ctx.Response.OutputStream.Close();
        }
    }
}
