using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace SUB4
{
    public class Program
    {
        static Dictionary<string, HashSet<string>> Models;
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
            var predValue = new Dictionary<string, int>();
            var predLatency = new Dictionary<string, int>();
            var pTs = new Dictionary<string, string>();
            var act = new Dictionary<string, int>();

            foreach (var r in Records)
            {
                var aid = (string)r["agentId"];
                if (!agents.Contains(aid)) continue;
                var key = aid + "|" + (string)r["requestId"];
                var type = (string)r["dataType"];
                if (type == "P")
                {
                    predValue[key]   = (int)r["dataValue"];
                    predLatency[key] = (int)r["latency"];
                    pTs[key]         = (string)r["timestamp"];
                }
                else
                {
                    act[key] = (int)r["dataValue"];
                }
            }

            int correct = 0, total = 0;
            long latencySum = 0;
            foreach (var kv in predValue)
            {
                if (!pTs[kv.Key].StartsWith(timeWindow)) continue;
                total++;
                latencySum += predLatency[kv.Key];
                int a;
                if (act.TryGetValue(kv.Key, out a) && a == kv.Value) correct++;
            }
            int latency = total > 0 ? (int)(latencySum / total) : 0;
            return JsonConvert.SerializeObject(new { correct = correct, total = total, latency = latency });
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
