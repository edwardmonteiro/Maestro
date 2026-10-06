import { afterEach, expect, it, vi } from 'vitest';
import { EventType, type RunAgentInput } from '@ag-ui/core';
import { lastValueFrom, toArray } from 'rxjs';
import { DotAgent } from '../src/server/dot-agent.js';
import { Store } from '../src/server/store.js';
import { WorkspaceStore } from '../src/server/workspace.js';

afterEach(() => vi.restoreAllMocks());
function response(output: Record<string, unknown>[]) {
  const events: Record<string, unknown>[] = [{ type: 'response.created', response: {id:'resp_test',model:'gpt-6-astra',output:[]} }];
  output.forEach((item, index) => {
    events.push({ type:'response.output_item.added',output_index:index,item });
    if(item.type === 'function_call') {
      events.push({type:'response.function_call_arguments.delta',output_index:index,item_id:item.id,delta:item.arguments});
      events.push({type:'response.function_call_arguments.done',output_index:index,item_id:item.id,arguments:item.arguments});
    }
    if(item.type === 'message') {
      events.push({type:'response.output_text.delta',output_index:index,item_id:item.id,content_index:0,delta:'Notas criadas.'});
    }
    events.push({type:'response.output_item.done',output_index:index,item});
  });
  events.push({type:'response.completed',response:{id:'resp_test',model:'gpt-6-astra',status:'completed',output,usage:{input_tokens:10,output_tokens:20,total_tokens:30}}});
  return new Response(events.map((event,i)=>`event: ${event.type}\ndata: ${JSON.stringify({...event,sequence_number:i})}\n\n`).join(''),{headers:{'Content-Type':'text/event-stream'}});
}
it('Astra uses Responses/high and preserves encrypted reasoning across a real tool loop', async () => {
  const store = new Store(':memory:');
  const workspace = new WorkspaceStore(':memory:', 'owner');
  try {
    const dot = workspace.dots()[0];
    workspace.bindThread('thread',dot.id,'Astra');
    const agent = new DotAgent(store,workspace,{
      intelligenceKey:'fixture',apiKey:'fixture',model:'gpt-6-astra',baseUrl:'https://unused.invalid/v1',
      runtimeUrl:'',voiceName:'marin',slackUsers:[],
    },dot.id);
    const network = vi.spyOn(globalThis,'fetch')
      .mockResolvedValueOnce(response([
        {type:'reasoning',id:'rs_test',summary:[],encrypted_content:'encrypted-fixture'},
        {type:'function_call',id:'fc_test',call_id:'call_test',name:'create_space_page',arguments:JSON.stringify({title:'Notas',content:'# Notas'})},
      ]))
      .mockResolvedValueOnce(response([{type:'message',id:'msg_test',role:'assistant',status:'completed',content:[{type:'output_text',text:'Notas criadas.',annotations:[]}]}]));
    const input: RunAgentInput = {threadId:'thread',runId:'run',state:{},context:[],
      messages:[{id:'user',role:'user',content:'Crie uma página Notas.'}],tools:[],forwardedProps:{}};
    const events = await lastValueFrom(agent.run(input).pipe(toArray()));
    expect(events.filter(e=>e.type===EventType.RUN_ERROR)).toEqual([]);
    expect(workspace.pages.list(dot.spaceId)).toEqual(expect.arrayContaining([expect.objectContaining({title:'Notas'})]));
    expect(network).toHaveBeenCalledTimes(2);
    expect(String(network.mock.calls[0][0])).toBe('https://unused.invalid/v1/responses');
    const first=JSON.parse(String(network.mock.calls[0][1]?.body));
    expect(first.model).toBe('gpt-6-astra');expect(first.reasoning.effort).toBe('high');
    expect(first.max_output_tokens).toBe(25000);expect(first.store).toBe(false);
    expect(first.include).toContain('reasoning.encrypted_content');
    for(const field of ['temperature','top_p','max_completion_tokens','reasoning_effort']) expect(first).not.toHaveProperty(field);
    expect(first.tools).toContainEqual(expect.objectContaining({type:'function',name:'create_space_page'}));
    const next=JSON.parse(String(network.mock.calls[1][1]?.body));
    expect(next.input).toContainEqual(expect.objectContaining({type:'function_call_output',call_id:'call_test',output:expect.stringContaining('Notas')}));
    expect(next.input).toContainEqual(expect.objectContaining({type:'reasoning',encrypted_content:'encrypted-fixture'}));
    expect(events).toContainEqual(expect.objectContaining({type:EventType.TEXT_MESSAGE_CHUNK,delta:'Notas criadas.'}));
  } finally {workspace.close();store.close();}
});
