#include <iostream>
#include <vector>
#include <fstream>
#include <string>
#include <map>
#include <stdio.h>
#include <algorithm>
#include <cuda_runtime.h>
#include <helper_cuda.h>

using namespace std;

//#define N 5000  // number of nodes
//#define d 0.85  // damping factor used in pageRank algorithm
//#define INITIAL_RANK 1 / double(N)  // initial pageRank value
#define ITERATIONS 10


void logRanks(const float *rank, long nodes)
{
  for(int i = 1; i < nodes; ++i)
  {
    cout << i << " = ";
    cout.precision(5);
    cout << rank[i] << endl;
  }
  cout << endl;
}


__global__ void calculatePageRank(const float *d_rank, float *d_new_rank, const int *d_parents, const int *d_children_count, const int *d_start, const int *d_size, int nodes_num, float d)
{
  // PageRank algorithm formula:
  // PR(A) = (1-d) + d * ( PR(parent_1) / #children(parent_1) + ...) 

  int i = blockDim.x * blockIdx.x + threadIdx.x;

  float new_rank = 0;

  if (i < nodes_num)
  {
    int start_index = d_start[i];
    for (int offset = 0; offset < d_size[i]; ++offset)
    {
      int children_num = d_children_count[d_parents[start_index + offset]];
      if(children_num != 0)
        new_rank += d_rank[d_parents[start_index + offset]] / children_num;
    }

    d_new_rank[i] = (1 - d) + (d * new_rank);
  }
}


int main()
{

  map<string,long> urls;
  map<long,string> reverse_urls;
  map<long,vector<long> > temp_parents;
  map<long,int> temp_children_count;

  string line,value;
  int size;
  long nodes = 1;

  ifstream myfile("web_graph.txt",ios_base::in);

  if (myfile.is_open())
  {
    getline(myfile,line);
    do {
        int i = 2;
        string number = "";
        while(i < line.length() && line[i] != ' ')
        {
            number += line[i++];
        }
        size = stoi(number.c_str());


        getline(myfile,line);
        if(urls.count(line) == 0)
        {
          urls[line] = nodes;
          reverse_urls[nodes++] = line;
        }
        temp_children_count[urls[line]] = size;
        long index = urls[line];


        for(int j = 0 ; j < size ; ++j)
        {
            getline(myfile,value);
            if(urls.count(value) == 0)
            {
              urls[value] = nodes;
              reverse_urls[nodes++] = value;
            }
            temp_parents[urls[value]].push_back(index);
        }

    } while(getline(myfile,line));
      
    myfile.close();
  }

  else cout << "Unable to open file";


    // for (auto i = temp_parents.begin(); i != temp_parents.end(); ++i)
    // {
    //     cout << i->first << " : " << temp_children_count[i->first] << endl;
    //     for(int j = 0 ; j < i->second.size(); ++j)
    //         cout << (i->second)[j] << " ";
    //     cout << endl;
    // }



  int **parents = (int **)malloc(nodes * sizeof(int *));
  int *children_count = (int *)malloc(nodes * sizeof(int));


  long long edges = 0;
  for(int i = 1; i < nodes; ++i)
  {
    if(temp_parents.count(i) != 0)
    {
      parents[i] = (int *)malloc(temp_parents[i].size() * sizeof(int));

      for (int j = 0 ; j < temp_parents[i].size() ; j++)
      {
          parents[i][j] = temp_parents[i][j];
      }
    
      edges = edges + temp_parents[i].size();
    }
    if(temp_children_count.count(i) == 0)
      children_count[i] = 0;
    else children_count[i] = temp_children_count[i];
  }



  float *h_rank = (float *)malloc(sizeof(float) * nodes);
  int *h_parents = (int *)malloc(sizeof(int) * edges);
  int *h_children_count = (int *)malloc(sizeof(int) * nodes);
  int *h_start = (int *)malloc(sizeof(int) * nodes);
  int *h_size = (int *)malloc(sizeof(int) * nodes);

  // Verify that allocations succeeded
    if (h_rank == NULL || h_parents == NULL || h_children_count == NULL || h_start == NULL || h_size == NULL)
    {
        fprintf(stderr, "Failed to allocate host vectors!\n");
        exit(EXIT_FAILURE);
    }


    // Initialize arrays

  for(int i = 1; i < nodes; ++i)
    h_rank[i] = 1 / double(nodes - 1);

  for(int i = 1; i < nodes; ++i)
    h_children_count[i] = children_count[i];

  long long x = 0;
  for(int i = 1 ; i < nodes ;++i)
  {
    h_start[i] = x;
    int j = 0;
    while(j < temp_parents[i].size())
    {
      h_parents[x++] = parents[i][j++];
    }
    h_size[i] = x - h_start[i];
  }



  float *d_rank = NULL;
  float *d_new_rank = NULL;
  int *d_parents = NULL;
  int *d_children_count = NULL;
  int *d_start = NULL;
  int *d_size = NULL;


  cudaError_t err = cudaSuccess;

  err = cudaMalloc((void **)&d_rank, sizeof(float) * nodes);
  if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to allocate device vector d_rank (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

  err = cudaMalloc((void **)&d_new_rank, sizeof(float) * nodes);
  if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to allocate device vector d_new_rank (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

  err = cudaMalloc((void **)&d_parents, sizeof(int) * edges);
  if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to allocate device vector d_parents (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

  err = cudaMalloc((void **)&d_children_count, sizeof(int) * nodes);
  if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to allocate device vector d_children_count (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

  err = cudaMalloc((void **)&d_start, sizeof(int) * nodes);
  if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to allocate device vector d_start (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

  err = cudaMalloc((void **)&d_size, sizeof(int) * nodes);
  if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to allocate device vector d_size (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }






    printf("Copy input data from the host memory to the CUDA device\n");

    err = cudaMemcpy(d_parents, h_parents, sizeof(int) * edges, cudaMemcpyHostToDevice);
    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to copy vector parents from host to device (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

    err = cudaMemcpy(d_children_count, h_children_count, sizeof(int) * nodes, cudaMemcpyHostToDevice);
    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to copy vector children_count from host to device (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

    err = cudaMemcpy(d_start, h_start, sizeof(int) * nodes, cudaMemcpyHostToDevice);
    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to copy vector start from host to device (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

    err = cudaMemcpy(d_size, h_size, sizeof(int) * nodes, cudaMemcpyHostToDevice);
    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to copy vector size from host to device (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }


  logRanks(h_rank,nodes);


  float d = 0.85;

  cudaEvent_t start, stop;
  cudaEventCreate(&start);
  cudaEventCreate(&stop);



  for (int i = 0 ; i < ITERATIONS ; ++i)
  {

    err = cudaMemcpy(d_rank, h_rank, sizeof(float) * nodes, cudaMemcpyHostToDevice);
    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to copy vector rank from host to device (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

    int threadsPerBlock = 256;
    int blocksPerGrid = ((nodes - 1) / threadsPerBlock) + 1;
    printf("CUDA kernel launch with %d blocks of %d threads\n", blocksPerGrid, threadsPerBlock);


    // Launch kernel on GPU
    cudaEventRecord(start);
    calculatePageRank<<<blocksPerGrid, threadsPerBlock>>>
      (d_rank, d_new_rank, d_parents, d_children_count, d_start, d_size, nodes, d);
    cudaEventRecord(stop);

    err = cudaGetLastError();
    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to launch PageRank kernel (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }


    // Wait for GPU to finish before accessing on host
    cudaDeviceSynchronize();

    // Copy the device result vector in device memory to the host result vector
    // in host memory.
    printf("Copy output data from the CUDA device to the host memory\n");
    err = cudaMemcpy(h_rank, d_new_rank, sizeof(float) * nodes, cudaMemcpyDeviceToHost);

    if (err != cudaSuccess)
    {
        fprintf(stderr, "Failed to copy vector rank from device to host (error code %s)!\n", cudaGetErrorString(err));
        exit(EXIT_FAILURE);
    }

    logRanks(h_rank,nodes);

    cudaEventSynchronize(stop);

    float milliseconds = 0;
    cudaEventElapsedTime(&milliseconds, start, stop);
    cout << "Time elapsed in milliseconds: " << milliseconds << endl;
  }



  sort(h_rank + 1, h_rank + nodes, greater<float>());

  ofstream file("ranks.txt");
  if(file.is_open())
  {
    for(int i = 1; i < nodes; ++i)
    {
      file << reverse_urls[i] << endl;
      file.precision(5);
      file << h_rank[i] << endl;
    }
    file.close();
  }
  else cout << "Failed to open file to write!";

  // Free device global memory
  err = cudaFree(d_rank);
  if (err != cudaSuccess)
  {
      fprintf(stderr, "Failed to free device vector (error code %s)!\n", cudaGetErrorString(err));
      exit(EXIT_FAILURE);
  }

  err = cudaFree(d_new_rank);
  if (err != cudaSuccess)
  {
      fprintf(stderr, "Failed to free device vector (error code %s)!\n", cudaGetErrorString(err));
      exit(EXIT_FAILURE);
  }

  err = cudaFree(d_parents);
  if (err != cudaSuccess)
  {
      fprintf(stderr, "Failed to free device vector (error code %s)!\n", cudaGetErrorString(err));
      exit(EXIT_FAILURE);
  }

  err = cudaFree(d_children_count);
  if (err != cudaSuccess)
  {
      fprintf(stderr, "Failed to free device vector (error code %s)!\n", cudaGetErrorString(err));
      exit(EXIT_FAILURE);
  }

  err = cudaFree(d_start);
  if (err != cudaSuccess)
  {
      fprintf(stderr, "Failed to free device vector (error code %s)!\n", cudaGetErrorString(err));
      exit(EXIT_FAILURE);
  }

  err = cudaFree(d_size);
  if (err != cudaSuccess)
  {
      fprintf(stderr, "Failed to free device vector (error code %s)!\n", cudaGetErrorString(err));
      exit(EXIT_FAILURE);
  }

  // Free host memory
  free(h_rank);
  free(h_parents);
  free(h_children_count);
  free(h_start);
  free(h_size);

  printf("Done\n");

  return 0;
}
 